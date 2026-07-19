package com.example.timescaledbapistatsexample.infrastructure.persistence

import com.example.timescaledbapistatsexample.domain.model.ApiCallEventRecord
import com.example.timescaledbapistatsexample.support.TimescaleDbTestSupport
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance

/**
 * 저장 경로의 멱등성을 검증한다.
 *
 * consumer는 저장에 실패하면 ACK를 하지 않고 pending으로 남겨 나중에 XCLAIM으로 회수한다.
 * 즉 같은 메시지가 두 번 저장될 수 있고, 그 중복은 (stream_id, occurred_at) PK가 흡수해야 한다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JdbcApiCallEventStoreIntegrationTest {
    private lateinit var store: JdbcApiCallEventStore

    @BeforeAll
    fun startContainer() {
        assumeTrue(TimescaleDbTestSupport.dockerAvailable(), "Docker를 쓸 수 없어 통합 테스트를 건너뜁니다")
        store = JdbcApiCallEventStore(TimescaleDbTestSupport.jdbcTemplate)
    }

    @BeforeEach
    fun reset() {
        TimescaleDbTestSupport.resetEvents()
    }

    @Test
    fun `같은 stream_id를 다시 저장해도 행이 늘지 않는다`() {
        val at = Instant.now().minus(1, ChronoUnit.MINUTES)
        val batch = listOf(record("dup-1", at), record("dup-2", at))

        store.write(batch)
        store.write(batch)

        assertEquals(2, countEvents())
    }

    @Test
    fun `배치 안에 중복이 섞여 있어도 저장된다`() {
        val at = Instant.now().minus(1, ChronoUnit.MINUTES)

        store.write(listOf(record("a", at)))
        store.write(listOf(record("a", at), record("b", at)))

        assertEquals(2, countEvents())
    }

    @Test
    fun `빈 배치는 아무 것도 하지 않는다`() {
        store.write(emptyList())

        assertEquals(0, countEvents())
    }

    @Test
    fun `nullable 필드가 비어 있어도 저장된다`() {
        val at = Instant.now().minus(1, ChronoUnit.MINUTES)

        store.write(
            listOf(
                ApiCallEventRecord(
                    streamId = "anon-1",
                    occurredAt = at,
                    apiClientId = null,
                    apiClientName = null,
                    authResult = "MISSING_API_KEY",
                    deniedReason = "API key header is missing",
                    method = "GET",
                    path = "/api/products",
                    pathPattern = "/api/products",
                    status = 401,
                    durationMs = 1,
                    clientIp = null,
                    userAgent = null,
                    errorType = null,
                ),
            ),
        )

        assertEquals(1, countEvents())
    }

    private fun countEvents(): Int =
        TimescaleDbTestSupport.jdbcTemplate.queryForObject(
            "SELECT count(*) FROM api_call_events",
            emptyMap<String, Any>(),
            Int::class.java,
        ) ?: 0

    private fun record(streamId: String, at: Instant) = ApiCallEventRecord(
        streamId = streamId,
        occurredAt = at,
        apiClientId = 1,
        apiClientName = "demo-client-01",
        authResult = "ALLOWED",
        deniedReason = null,
        method = "GET",
        path = "/api/products",
        pathPattern = "/api/products",
        status = 200,
        durationMs = 10,
        clientIp = "127.0.0.1",
        userAgent = "integration-test",
        errorType = null,
    )
}
