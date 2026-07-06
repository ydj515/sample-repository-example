package com.example.timescaledbapistatsexample.application

import com.example.timescaledbapistatsexample.domain.model.ApiCallEvent
import com.example.timescaledbapistatsexample.domain.port.ApiCallEventStreamRepository
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiCallEventPublisherTest {
    private val repository = RecordingStreamRepository()

    // 워커 스레드는 시작하지 않고(=@PostConstruct 미호출) enqueue/drop/flush 로직만 결정적으로 검증한다.
    private val publisher = ApiCallEventPublisher(
        streamRepository = repository,
        queueCapacity = 2,
        batchSize = 10,
        pollTimeoutMs = 50,
    )

    private fun event(status: Int) = ApiCallEvent(
        occurredAt = Instant.parse("2026-07-07T00:00:00Z"),
        apiClientId = 1,
        apiClientName = "demo-client-01",
        authResult = "ALLOWED",
        deniedReason = null,
        method = "GET",
        path = "/api/products",
        pathPattern = "/api/products",
        status = status,
        durationMs = 1,
        clientIp = "127.0.0.1",
        userAgent = "k6",
        errorType = null,
    )

    @Test
    fun `큐가 가득 차면 초과분을 drop 하고 카운트한다`() {
        publisher.publish(event(200))
        publisher.publish(event(201))
        publisher.publish(event(202)) // capacity=2 초과

        assertEquals(2, publisher.queueSize())
        assertEquals(1, publisher.droppedCount())
    }

    @Test
    fun `drainAndFlush는 큐에 쌓인 이벤트를 배치로 발행한다`() {
        publisher.publish(event(200))
        publisher.publish(event(201))

        publisher.drainAndFlush()

        assertEquals(1, repository.appendedBatches.size)
        assertEquals(2, repository.appendedBatches.first().size)
        assertEquals(0, publisher.queueSize())
    }

    private class RecordingStreamRepository : ApiCallEventStreamRepository {
        val appendedBatches = mutableListOf<List<Map<String, String>>>()

        override fun appendAll(fieldsList: List<Map<String, String>>) {
            appendedBatches += fieldsList
        }

        override fun ensureConsumerGroup() = Unit

        override fun readNew(batchSize: Long, block: Duration) = emptyList<com.example.timescaledbapistatsexample.domain.model.StreamEntry>()

        override fun claimStale(minIdle: Duration, batchSize: Long) = emptyList<com.example.timescaledbapistatsexample.domain.model.StreamEntry>()

        override fun acknowledge(ids: List<String>) = Unit

        override fun trim(maxLen: Long) = Unit
    }
}
