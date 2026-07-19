package com.example.timescaledbapistatsexample.infrastructure.persistence

import com.example.timescaledbapistatsexample.domain.model.ApiCallEventRecord
import com.example.timescaledbapistatsexample.support.TimescaleDbTestSupport
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

/**
 * init SQL이 만든 TimescaleDB 구조 자체를 검증한다.
 *
 * 파일에 특정 문자열이 있는지 보는 게 아니라, 실제로 뜬 DB에 질의해서
 * hypertable / continuous aggregate / 압축 정책이 의도대로 걸렸는지 확인한다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimescaleDbSchemaIntegrationTest {
    private val jdbc get() = TimescaleDbTestSupport.jdbcTemplate

    @BeforeAll
    fun startContainer() {
        assumeTrue(TimescaleDbTestSupport.dockerAvailable(), "Docker를 쓸 수 없어 통합 테스트를 건너뜁니다")
        TimescaleDbTestSupport.container
    }

    @Test
    fun `api_call_events는 1일 청크 hypertable이다`() {
        val interval = jdbc.queryForObject(
            """
            SELECT time_interval
            FROM timescaledb_information.dimensions
            WHERE hypertable_name = 'api_call_events' AND column_name = 'occurred_at'
            """.trimIndent(),
            emptyMap<String, Any>(),
            String::class.java,
        )

        assertEquals("1 day", interval)
    }

    @Test
    fun `시간 인덱스가 중복 생성되지 않는다`() {
        // create_hypertable이 (occurred_at DESC) 인덱스를 자동으로 만든다.
        // init SQL에서 같은 정의를 또 만들면 쓰기마다 불필요한 인덱스 비용이 붙는다.
        val duplicated = jdbc.queryForObject(
            """
            SELECT count(*)
            FROM pg_indexes
            WHERE tablename = 'api_call_events'
              AND indexdef ILIKE '%(occurred_at DESC)%'
            """.trimIndent(),
            emptyMap<String, Any>(),
            Int::class.java,
        )

        assertEquals(1, duplicated, "occurred_at DESC 인덱스는 하나만 있어야 합니다")
    }

    @Test
    fun `세 continuous aggregate 모두 realtime aggregation이 켜져 있다`() {
        val rows = jdbc.queryForList(
            """
            SELECT view_name, materialized_only
            FROM timescaledb_information.continuous_aggregates
            ORDER BY view_name
            """.trimIndent(),
            emptyMap<String, Any>(),
        )

        assertEquals(
            listOf("api_key_call_stats_daily", "api_key_call_stats_monthly", "api_key_call_stats_yearly"),
            rows.map { it["view_name"] },
        )
        assertTrue(
            rows.all { it["materialized_only"] == false },
            "materialized_only가 true면 최신 구간이 조회에서 빠집니다",
        )
    }

    @Test
    fun `월-년 집계는 raw가 아니라 한 단계 아래 집계를 읽는다`() {
        // 계층형이 아니면 상위 집계가 매번 raw 전체를 재스캔한다.
        val definitions = jdbc.queryForList(
            """
            SELECT view_name, view_definition
            FROM timescaledb_information.continuous_aggregates
            """.trimIndent(),
            emptyMap<String, Any>(),
        ).associate { it["view_name"] as String to (it["view_definition"] as String) }

        assertTrue(
            definitions.getValue("api_key_call_stats_daily").contains("api_call_events"),
            "daily는 raw hypertable을 읽어야 합니다",
        )
        assertTrue(
            definitions.getValue("api_key_call_stats_monthly").contains("api_key_call_stats_daily"),
            "monthly는 daily를 롤업해야 합니다",
        )
        assertTrue(
            definitions.getValue("api_key_call_stats_yearly").contains("api_key_call_stats_monthly"),
            "yearly는 monthly를 롤업해야 합니다",
        )
    }

    @Test
    fun `압축과 보존 정책이 등록되어 있다`() {
        val policies = jdbc.queryForList(
            """
            SELECT proc_name
            FROM timescaledb_information.jobs
            WHERE hypertable_name = 'api_call_events'
            """.trimIndent(),
            emptyMap<String, Any>(),
        ).map { it["proc_name"] as String }

        assertTrue(policies.contains("policy_compression"), "압축 정책이 없습니다: $policies")
        assertTrue(policies.contains("policy_retention"), "보존 정책이 없습니다: $policies")
    }

    @Test
    fun `오래된 청크를 압축해도 집계 결과가 유지된다`() {
        TimescaleDbTestSupport.resetEvents()

        val old = Instant.now().minus(30, ChronoUnit.DAYS)
        val store = JdbcApiCallEventStore(jdbc)
        store.write(
            (1..200).map {
                ApiCallEventRecord(
                    streamId = "old-$it",
                    occurredAt = old,
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
            },
        )
        TimescaleDbTestSupport.refreshAllAggregates()

        val before = totalCalls()
        assertEquals(200, before)

        jdbc.jdbcTemplate.execute(
            """
            SELECT compress_chunk(c, if_not_compressed => true)
            FROM show_chunks('api_call_events', older_than => INTERVAL '7 days') c
            """.trimIndent(),
        )

        val compressed = jdbc.queryForObject(
            """
            SELECT count(*) FROM timescaledb_information.chunks
            WHERE hypertable_name = 'api_call_events' AND is_compressed
            """.trimIndent(),
            emptyMap<String, Any>(),
            Int::class.java,
        )

        assertTrue((compressed ?: 0) > 0, "오래된 청크가 압축되어야 합니다")
        assertEquals(before, totalCalls(), "압축 후에도 집계 값이 같아야 합니다")
    }

    private fun totalCalls(): Long =
        jdbc.queryForObject(
            "SELECT coalesce(sum(total_calls), 0) FROM api_key_call_stats_daily",
            emptyMap<String, Any>(),
            Long::class.java,
        ) ?: 0
}
