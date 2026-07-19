package com.example.timescaledbapistatsexample.infrastructure.persistence

import com.example.timescaledbapistatsexample.domain.model.ApiCallEventRecord
import com.example.timescaledbapistatsexample.domain.model.StatsPeriod
import com.example.timescaledbapistatsexample.domain.model.StatsSource
import com.example.timescaledbapistatsexample.support.TimescaleDbTestSupport
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance

/**
 * 실제 TimescaleDB에 붙어서 통계 조회 SQL을 검증한다.
 *
 * 이 테스트가 없으면 raw/aggregate 분기, 버킷 경계 처리, 롤업 정확도처럼
 * "SQL이 DB에 닿아야만 드러나는" 문제를 잡을 수 없다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiStatsRepositoryIntegrationTest {
    private lateinit var repository: ApiStatsRepository
    private lateinit var store: JdbcApiCallEventStore

    @BeforeAll
    fun startContainer() {
        assumeTrue(TimescaleDbTestSupport.dockerAvailable(), "Docker를 쓸 수 없어 통합 테스트를 건너뜁니다")
        repository = ApiStatsRepository(TimescaleDbTestSupport.jdbcTemplate)
        store = JdbcApiCallEventStore(TimescaleDbTestSupport.jdbcTemplate)
    }

    @BeforeEach
    fun reset() {
        TimescaleDbTestSupport.resetEvents()
    }

    @Test
    fun `refresh 없이도 continuous aggregate가 최신 이벤트를 돌려준다`() {
        // realtime aggregation이 꺼져 있으면(materialized_only = true) 이 테스트가 0건으로 실패한다.
        //
        // materialization watermark보다 뒤(미래)의 시각을 쓴다.
        // 운영에서 이벤트가 도착하는 위치와 같은 관계다.
        // 정책의 end_offset(1분)만큼 watermark가 항상 현재보다 뒤처져 있으므로,
        // 새로 들어온 이벤트는 언제나 watermark 이후 구간에 놓이고 실시간 계산으로 조회된다.
        val ahead = Instant.now().plus(2, ChronoUnit.DAYS)
        store.write(records(count = 50, at = ahead))

        val aggregate = repository.apiKeyCalls(
            period = StatsPeriod.DAY,
            source = StatsSource.AGGREGATE,
            from = ahead.minus(1, ChronoUnit.DAYS),
            to = ahead.plus(1, ChronoUnit.DAYS),
            apiClientId = null, method = null, pathPattern = null, limit = 100,
        )

        assertEquals(50, aggregate.sumOf { it.totalCalls })
    }

    @Test
    fun `raw와 aggregate가 모든 기간에서 같은 결과를 준다`() {
        store.write(records(count = 120, at = Instant.now().minus(3, ChronoUnit.MINUTES)))
        TimescaleDbTestSupport.refreshAllAggregates()

        val from = Instant.now().minus(1, ChronoUnit.DAYS)
        val to = Instant.now().plus(1, ChronoUnit.DAYS)

        // period=month, year는 버킷 라벨이 월/년의 1일이라 경계 스냅이 없으면 aggregate만 0건이 된다.
        StatsPeriod.entries.forEach { period ->
            val raw = repository.apiKeyCalls(
                period, StatsSource.RAW, from, to, null, null, null, 100,
            )
            val aggregate = repository.apiKeyCalls(
                period, StatsSource.AGGREGATE, from, to, null, null, null, 100,
            )

            assertEquals(raw, aggregate, "period=${period.queryValue}에서 raw와 aggregate가 다릅니다")
            assertEquals(120, raw.sumOf { it.totalCalls }, "period=${period.queryValue} 총 호출 수")
        }
    }

    @Test
    fun `계층형 롤업이 가중평균을 정확히 복원한다`() {
        // 평균을 저장했다면 상위 단계에서 "평균의 평균"이 되어 값이 어긋난다.
        val at = Instant.now().minus(5, ChronoUnit.MINUTES)
        store.write(
            listOf(
                record("a", at, durationMs = 10),
                record("b", at, durationMs = 20),
                record("c", at, durationMs = 300),
            ),
        )
        TimescaleDbTestSupport.refreshAllAggregates()

        val from = Instant.now().minus(1, ChronoUnit.DAYS)
        val to = Instant.now().plus(1, ChronoUnit.DAYS)
        val expectedAverage = (10 + 20 + 300).toDouble() / 3

        StatsPeriod.entries.forEach { period ->
            val stat = repository.apiKeyCalls(
                period, StatsSource.AGGREGATE, from, to, null, null, null, 100,
            ).single()

            assertEquals(expectedAverage, stat.averageDurationMs, 0.0001, "period=${period.queryValue} 평균")
            assertEquals(300, stat.maxDurationMs, "period=${period.queryValue} 최대")
        }
    }

    @Test
    fun `limit은 버킷별 상위 N개로 적용된다`() {
        // 전체 행 수로 자르면 앞쪽 버킷만 채우고 뒤쪽 버킷이 통째로 사라진다.
        val today = Instant.now().minus(2, ChronoUnit.HOURS)
        val yesterday = today.minus(1, ChronoUnit.DAYS)

        val events = buildList {
            listOf(today, yesterday).forEach { at ->
                (1..3).forEach { clientId ->
                    repeat(clientId * 2) { seq ->
                        add(record("c$clientId-$at-$seq", at, clientId = clientId.toLong()))
                    }
                }
            }
        }
        store.write(events)
        TimescaleDbTestSupport.refreshAllAggregates()

        val stats = repository.apiKeyCalls(
            period = StatsPeriod.DAY,
            source = StatsSource.AGGREGATE,
            from = Instant.now().minus(3, ChronoUnit.DAYS),
            to = Instant.now().plus(1, ChronoUnit.DAYS),
            apiClientId = null, method = null, pathPattern = null,
            limit = 1,
        )

        val buckets = stats.map { it.bucket }.distinct()
        assertEquals(2, buckets.size, "두 버킷 모두 결과에 남아야 합니다")
        assertEquals(2, stats.size, "버킷마다 상위 1건씩만 나와야 합니다")
        // 각 버킷의 1위는 호출이 가장 많은 client 3 (6건)
        assertTrue(stats.all { it.totalCalls == 6L }, "버킷별 최상위 행이 선택되어야 합니다")
    }

    @Test
    fun `apiClientId 필터가 적용된다`() {
        val at = Instant.now().minus(5, ChronoUnit.MINUTES)
        store.write(
            listOf(
                record("x1", at, clientId = 1),
                record("x2", at, clientId = 2),
                record("x3", at, clientId = 2),
            ),
        )
        TimescaleDbTestSupport.refreshAllAggregates()

        val stats = repository.apiKeyCalls(
            period = StatsPeriod.DAY,
            source = StatsSource.AGGREGATE,
            from = Instant.now().minus(1, ChronoUnit.DAYS),
            to = Instant.now().plus(1, ChronoUnit.DAYS),
            apiClientId = 2, method = null, pathPattern = null, limit = 100,
        )

        assertEquals(1, stats.size)
        assertEquals(2, stats.single().apiClientId)
        assertEquals(2, stats.single().totalCalls)
    }

    private fun records(count: Int, at: Instant): List<ApiCallEventRecord> =
        (1..count).map { record("evt-$it", at) }

    private fun record(
        streamId: String,
        at: Instant,
        clientId: Long = 1,
        durationMs: Long = 10,
        status: Int = 200,
    ) = ApiCallEventRecord(
        streamId = streamId,
        occurredAt = at,
        apiClientId = clientId,
        apiClientName = "demo-client-%02d".format(clientId),
        authResult = "ALLOWED",
        deniedReason = null,
        method = "GET",
        path = "/api/products",
        pathPattern = "/api/products",
        status = status,
        durationMs = durationMs,
        clientIp = "127.0.0.1",
        userAgent = "integration-test",
        errorType = null,
    )
}
