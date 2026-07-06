package com.example.timescaledbapistatsexample.infrastructure.redis

import com.example.timescaledbapistatsexample.domain.model.StreamEntry
import com.example.timescaledbapistatsexample.domain.port.ApiCallEventStreamRepository
import com.example.timescaledbapistatsexample.support.hasMessageInChain
import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.connection.stream.StreamReadOptions
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

/**
 * [ApiCallEventStreamRepository]의 Redis Stream 구현체(adapter).
 * 스트림 key, consumer group, consumer name 같은 Redis 고유 설정은 이 클래스에 캡슐화한다.
 */
@Repository
class RedisApiCallEventStreamRepository(
    private val redisTemplate: StringRedisTemplate,
    @Value("\${api-stats.redis.stream-key}") private val streamKey: String,
    @Value("\${api-stats.redis.group}") private val group: String,
    @Value("\${api-stats.redis.consumer-name}") private val consumerName: String,
) : ApiCallEventStreamRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    private val streamOps
        get() = redisTemplate.opsForStream<String, String>()

    override fun append(fields: Map<String, String>) {
        val record = MapRecord.create(streamKey, fields).withId(RecordId.autoGenerate())
        streamOps.add(record)
    }

    override fun ensureConsumerGroup() {
        runCatching {
            redisTemplate.execute { connection ->
                connection.streamCommands().xGroupCreate(
                    streamKey.toByteArray(Charsets.UTF_8),
                    group,
                    ReadOffset.from("0-0"),
                    true,
                )
            }
        }.onFailure { ex ->
            if (ex.hasMessageInChain("BUSYGROUP")) {
                log.debug("Redis Stream consumer group already exists: {}", group)
            } else {
                log.warn("Failed to create Redis Stream consumer group", ex)
            }
        }
    }

    override fun readNew(batchSize: Long, block: Duration): List<StreamEntry> {
        return streamOps.read(
            Consumer.from(group, consumerName),
            StreamReadOptions.empty().count(batchSize).block(block),
            StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
        ).orEmpty().map { it.toStreamEntry() }
    }

    override fun claimStale(minIdle: Duration, batchSize: Long): List<StreamEntry> {
        val pending = streamOps.pending(streamKey, group, Range.unbounded<String>(), batchSize)

        val staleIds = mutableListOf<RecordId>()
        for (message in pending) {
            if (message.elapsedTimeSinceLastDelivery >= minIdle) {
                staleIds += message.id
            }
        }
        if (staleIds.isEmpty()) return emptyList()

        return streamOps.claim(streamKey, group, consumerName, minIdle, *staleIds.toTypedArray())
            .map { it.toStreamEntry() }
    }

    override fun acknowledge(ids: List<String>) {
        if (ids.isEmpty()) return
        val recordIds = ids.map { RecordId.of(it) }.toTypedArray()
        streamOps.acknowledge(streamKey, group, *recordIds)
    }

    override fun trim(maxLen: Long) {
        streamOps.trim(streamKey, maxLen, true)
    }

    private fun MapRecord<String, String, String>.toStreamEntry(): StreamEntry {
        return StreamEntry(id = id.value, fields = value)
    }
}
