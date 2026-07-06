package com.example.timescaledbapistatsexample.application

import com.example.timescaledbapistatsexample.domain.model.ApiCallEvent
import com.example.timescaledbapistatsexample.domain.model.toStreamFields
import com.example.timescaledbapistatsexample.domain.port.ApiCallEventStreamRepository
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 요청 스레드를 Redis I/O와 분리하는 비동기 발행자.
 *
 * - 요청 스레드는 bounded 큐에 offer만 한다(논블로킹, 큐가 차면 drop).
 * - 전용 워커 스레드가 큐를 배치로 비워 파이프라인 XADD(왕복 1회)로 발행한다.
 * - 종료 시 남은 큐를 flush 해 정상 종료 시 유실을 막는다(하드킬은 인메모리 특성상 유실 가능).
 */
@Component
class ApiCallEventPublisher(
    private val streamRepository: ApiCallEventStreamRepository,
    @Value("\${api-stats.publisher.queue-capacity:10000}") queueCapacity: Int,
    @Value("\${api-stats.publisher.batch-size:500}") private val batchSize: Int,
    @Value("\${api-stats.publisher.poll-timeout-ms:200}") private val pollTimeoutMs: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val queue = ArrayBlockingQueue<ApiCallEvent>(queueCapacity)
    private val droppedCount = AtomicLong()

    @Volatile
    private var running = false
    private var worker: Thread? = null

    /**
     * 요청 스레드에서 호출된다. 큐가 가득 차면 블로킹하지 않고 drop 후 카운트만 올린다.
     * 텔레메트리가 본 트래픽을 역으로 밀어내지 않도록 하는 명시적 backpressure 정책이다.
     */
    fun publish(event: ApiCallEvent) {
        if (!queue.offer(event)) {
            val dropped = droppedCount.incrementAndGet()
            if (dropped == 1L || dropped % 1000 == 0L) {
                log.warn("API call event queue is full; dropped {} events so far", dropped)
            }
        }
    }

    @PostConstruct
    fun start() {
        running = true
        worker = Thread({ runLoop() }, "api-call-event-publisher").apply {
            isDaemon = true
            start()
        }
    }

    @PreDestroy
    fun stop() {
        running = false
        worker?.interrupt()
        worker?.join(TimeUnit.SECONDS.toMillis(5))
        // 정상 종료: 남은 이벤트를 마지막으로 flush 해 유실을 막는다.
        drainAndFlush()
    }

    private fun runLoop() {
        while (running) {
            try {
                val first = queue.poll(pollTimeoutMs, TimeUnit.MILLISECONDS) ?: continue
                val batch = ArrayList<ApiCallEvent>(batchSize)
                batch.add(first)
                queue.drainTo(batch, batchSize - 1)
                flush(batch)
            } catch (ex: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (ex: Exception) {
                log.warn("Unexpected error in API call event publisher loop", ex)
            }
        }
    }

    /** 큐에 남은 이벤트를 모두 배치로 flush 한다(종료/테스트용). */
    fun drainAndFlush() {
        while (true) {
            val batch = ArrayList<ApiCallEvent>(batchSize)
            queue.drainTo(batch, batchSize)
            if (batch.isEmpty()) break
            flush(batch)
        }
    }

    private fun flush(events: List<ApiCallEvent>) {
        if (events.isEmpty()) return
        runCatching {
            streamRepository.appendAll(events.map { it.toStreamFields() })
        }.onFailure { ex ->
            log.warn("Failed to publish {} API call events to Redis Stream", events.size, ex)
        }
    }

    fun droppedCount(): Long = droppedCount.get()

    fun queueSize(): Int = queue.size
}
