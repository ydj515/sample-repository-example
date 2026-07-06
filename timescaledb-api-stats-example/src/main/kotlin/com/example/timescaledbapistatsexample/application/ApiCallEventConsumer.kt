package com.example.timescaledbapistatsexample.application

import com.example.timescaledbapistatsexample.domain.model.StreamEntry
import com.example.timescaledbapistatsexample.domain.port.ApiCallEventStore
import com.example.timescaledbapistatsexample.domain.port.ApiCallEventStreamRepository
import jakarta.annotation.PostConstruct
import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "api-stats.consumer", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class ApiCallEventConsumer(
    private val streamRepository: ApiCallEventStreamRepository,
    private val store: ApiCallEventStore,
    @Value("\${api-stats.redis.max-len}") private val maxLen: Long,
    @Value("\${api-stats.consumer.batch-size}") private val batchSize: Long,
    @Value("\${api-stats.consumer.poll-timeout-ms}") private val pollTimeoutMs: Long,
    @Value("\${api-stats.consumer.reclaim.enabled:true}") private val reclaimEnabled: Boolean,
    @Value("\${api-stats.consumer.reclaim.min-idle-ms:60000}") private val reclaimMinIdleMs: Long,
    @Value("\${api-stats.consumer.reclaim.batch-size:100}") private val reclaimBatchSize: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun createConsumerGroup() {
        streamRepository.ensureConsumerGroup()
    }

    @Scheduled(fixedDelayString = "\${api-stats.consumer.fixed-delay-ms}")
    fun consume() {
        runCatching {
            val entries = streamRepository.readNew(batchSize, Duration.ofMillis(pollTimeoutMs))
            persist(entries)
        }.onFailure { ex ->
            log.warn("Failed to consume API call events from Redis Stream", ex)
        }

        trimStream()
    }

    /**
     * 전달된 뒤 ACK되지 못한 채 idle 상태로 남은 pending 메시지를 회수해 재저장한다.
     * consumer가 read와 ACK 사이에 죽는 경우의 이벤트 유실을 복구한다.
     */
    @Scheduled(fixedDelayString = "\${api-stats.consumer.reclaim.fixed-delay-ms:30000}")
    fun reclaimStalePending() {
        if (!reclaimEnabled) return

        runCatching {
            val entries = streamRepository.claimStale(Duration.ofMillis(reclaimMinIdleMs), reclaimBatchSize)
            if (entries.isNotEmpty()) {
                log.info("Reclaimed {} stale pending Redis Stream entries", entries.size)
                persist(entries)
            }
        }.onFailure { ex ->
            log.warn("Failed to reclaim stale pending Redis Stream entries", ex)
        }
    }

    private fun persist(entries: List<StreamEntry>) {
        if (entries.isEmpty()) return

        val parsed = StreamEntryParser.parse(entries)

        // 변환 불가 메시지는 재시도해도 실패하므로 skip 처리로 ACK해 pending 적체를 막는다.
        if (parsed.invalidIds.isNotEmpty()) {
            streamRepository.acknowledge(parsed.invalidIds)
        }

        // 저장이 실패하면 ACK하지 않아 pending으로 남기고 회수 대상으로 둔다.
        // 재처리 시 중복은 (stream_id, occurred_at) primary key로 흡수된다.
        if (parsed.records.isNotEmpty()) {
            store.write(parsed.records)
            streamRepository.acknowledge(parsed.validIds)
        }
    }

    private fun trimStream() {
        runCatching {
            streamRepository.trim(maxLen)
        }.onFailure { ex ->
            log.debug("Failed to trim Redis Stream: {}", ex.message)
        }
    }
}
