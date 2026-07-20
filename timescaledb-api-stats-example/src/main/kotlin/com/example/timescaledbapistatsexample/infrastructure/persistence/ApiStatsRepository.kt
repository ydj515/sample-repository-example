package com.example.timescaledbapistatsexample.infrastructure.persistence

import com.example.timescaledbapistatsexample.domain.model.AuthFailure
import com.example.timescaledbapistatsexample.domain.model.ApiKeyCallStat
import com.example.timescaledbapistatsexample.domain.model.BucketCount
import com.example.timescaledbapistatsexample.domain.model.BucketFailureRate
import com.example.timescaledbapistatsexample.domain.model.BucketLatency
import com.example.timescaledbapistatsexample.domain.model.ClientCall
import com.example.timescaledbapistatsexample.domain.model.StatsPeriod
import com.example.timescaledbapistatsexample.domain.model.StatsSource
import com.example.timescaledbapistatsexample.domain.model.TopEndpoint
import com.example.timescaledbapistatsexample.domain.port.ApiStatsReader
import java.sql.Timestamp
import java.time.Instant
import java.time.OffsetDateTime
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ApiStatsRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : ApiStatsReader {
    override fun calls(bucket: String, from: Instant, to: Instant): List<BucketCount> {
        return jdbcTemplate.query(
            """
            SELECT time_bucket(CAST(:bucket AS interval), occurred_at) AS bucket, count(*) AS total_calls
            FROM api_call_events
            WHERE occurred_at >= :from AND occurred_at < :to
            GROUP BY bucket
            ORDER BY bucket
            """.trimIndent(),
            params(bucket, from, to),
        ) { rs, _ ->
            BucketCount(rs.getObject("bucket", OffsetDateTime::class.java).toInstant(), rs.getLong("total_calls"))
        }
    }

    override fun latency(bucket: String, from: Instant, to: Instant): List<BucketLatency> {
        return jdbcTemplate.query(
            """
            SELECT time_bucket(CAST(:bucket AS interval), occurred_at) AS bucket,
                   avg(duration_ms) AS average_duration_ms,
                   max(duration_ms) AS max_duration_ms
            FROM api_call_events
            WHERE occurred_at >= :from AND occurred_at < :to
            GROUP BY bucket
            ORDER BY bucket
            """.trimIndent(),
            params(bucket, from, to),
        ) { rs, _ ->
            BucketLatency(
                bucket = rs.getObject("bucket", OffsetDateTime::class.java).toInstant(),
                averageDurationMs = rs.getDouble("average_duration_ms"),
                maxDurationMs = rs.getLong("max_duration_ms"),
            )
        }
    }

    override fun failureRate(bucket: String, from: Instant, to: Instant): List<BucketFailureRate> {
        return jdbcTemplate.query(
            """
            SELECT time_bucket(CAST(:bucket AS interval), occurred_at) AS bucket,
                   count(*) AS total_calls,
                   count(*) FILTER (WHERE status >= 400) AS failed_calls
            FROM api_call_events
            WHERE occurred_at >= :from AND occurred_at < :to
            GROUP BY bucket
            ORDER BY bucket
            """.trimIndent(),
            params(bucket, from, to),
        ) { rs, _ ->
            val totalCalls = rs.getLong("total_calls")
            val failedCalls = rs.getLong("failed_calls")
            BucketFailureRate(
                bucket = rs.getObject("bucket", OffsetDateTime::class.java).toInstant(),
                totalCalls = totalCalls,
                failedCalls = failedCalls,
                failureRate = if (totalCalls == 0L) 0.0 else failedCalls.toDouble() / totalCalls.toDouble(),
            )
        }
    }

    /**
     * 같은 통계를 두 경로로 뽑는다.
     *
     * - [StatsSource.RAW]: hypertable을 조회 시점에 time_bucket으로 집계한다.
     * - [StatsSource.AGGREGATE]: 미리 말아둔 continuous aggregate를 읽는다.
     *
     * 두 분기는 같은 필터/정렬/LIMIT과 같은 컬럼 별칭을 쓰므로 결과를 그대로 비교할 수 있다.
     */
    override fun apiKeyCalls(
        period: StatsPeriod,
        source: StatsSource,
        from: Instant,
        to: Instant,
        apiClientId: Long?,
        method: String?,
        pathPattern: String?,
        limit: Int,
    ): List<ApiKeyCallStat> {
        // CAGG는 이미 버킷 단위로 말려 있어 시간 컬럼 이름과 집계 함수가 다르다.
        val timeColumn = if (source == StatsSource.AGGREGATE) "bucket" else "occurred_at"
        val parameters = params(from, to)
            .addValue("limit", limit)
            .addValue("bucket", period.bucket)
            .addValue("maxTotalRows", ApiStatsReader.MAX_TOTAL_ROWS)

        // 조회 구간을 버킷 경계로 스냅한다.
        //
        // raw는 이벤트 시각(occurred_at)에, CAGG는 버킷 시작 시각(bucket)에 필터가 걸린다.
        // 예를 들어 period=month에서 7월 버킷의 라벨은 2026-07-01인데, from이 2026-07-18이면
        // 원본 이벤트는 구간 안에 있어도 버킷 라벨이 구간 밖이라 CAGG 쪽만 결과가 통째로 비어버린다.
        //
        // 양쪽 모두 "구간에 걸친 완전한 버킷"을 대상으로 맞추면 두 경로의 결과가 일치한다.
        val bucketStart = "time_bucket(CAST(:bucket AS interval), CAST(:from AS timestamptz))"
        val bucketEnd =
            "time_bucket(CAST(:bucket AS interval), CAST(:to AS timestamptz)) + CAST(:bucket AS interval)"
        val conditions = mutableListOf(
            "$timeColumn >= $bucketStart",
            "$timeColumn < $bucketEnd",
        )

        if (apiClientId != null) {
            conditions += "api_client_id = :apiClientId"
            parameters.addValue("apiClientId", apiClientId)
        }
        if (method != null) {
            conditions += "method = :method"
            parameters.addValue("method", method)
        }
        if (pathPattern != null) {
            conditions += "path_pattern = :pathPattern"
            parameters.addValue("pathPattern", pathPattern)
        }

        val whereClause = conditions.joinToString("\n              AND ")
        val aggregationSql = when (source) {
            StatsSource.RAW ->
                """
                SELECT time_bucket(CAST(:bucket AS interval), occurred_at) AS bucket,
                       api_client_id,
                       coalesce(api_client_name, 'anonymous') AS api_client_name,
                       method,
                       path_pattern,
                       count(*) AS total_calls,
                       count(*) FILTER (WHERE status >= 400) AS failed_calls,
                       sum(duration_ms) AS total_duration_ms,
                       max(duration_ms) AS max_duration_ms
                FROM api_call_events
                WHERE $whereClause
                GROUP BY bucket, api_client_id, coalesce(api_client_name, 'anonymous'), method, path_pattern
                """.trimIndent()

            // period.aggregateView는 enum 상수라 사용자 입력이 아니다.
            StatsSource.AGGREGATE ->
                """
                SELECT bucket,
                       api_client_id,
                       coalesce(api_client_name, 'anonymous') AS api_client_name,
                       method,
                       path_pattern,
                       sum(total_calls) AS total_calls,
                       sum(failed_calls) AS failed_calls,
                       sum(total_duration_ms) AS total_duration_ms,
                       max(max_duration_ms) AS max_duration_ms
                FROM ${period.aggregateView}
                WHERE $whereClause
                GROUP BY bucket, api_client_id, coalesce(api_client_name, 'anonymous'), method, path_pattern
                """.trimIndent()
        }

        // limit을 전체 행 수에 걸면 앞쪽 버킷만 채우고 뒤쪽 버킷이 통째로 잘린다.
        // 버킷별 상위 N개를 뽑도록 row_number()로 순위를 매긴 뒤 자른다.
        val sql = """
            WITH aggregated AS (
            $aggregationSql
            ), ranked AS (
                SELECT aggregated.*,
                       row_number() OVER (
                           PARTITION BY bucket
                           ORDER BY total_calls DESC, api_client_name, method, path_pattern
                       ) AS bucket_rank
                FROM aggregated
            )
            SELECT *
            FROM ranked
            WHERE bucket_rank <= :limit
            ORDER BY bucket, total_calls DESC, api_client_name, method, path_pattern
            LIMIT :maxTotalRows
        """.trimIndent()

        return jdbcTemplate.query(sql, parameters) { rs, _ ->
            val totalCalls = rs.getLong("total_calls")
            val failedCalls = rs.getLong("failed_calls")
            val totalDurationMs = rs.getLong("total_duration_ms")
            ApiKeyCallStat(
                bucket = rs.getObject("bucket", OffsetDateTime::class.java).toInstant(),
                apiClientId = rs.getObject("api_client_id")?.let { (it as Number).toLong() },
                apiClientName = rs.getString("api_client_name"),
                method = rs.getString("method"),
                pathPattern = rs.getString("path_pattern"),
                totalCalls = totalCalls,
                failedCalls = failedCalls,
                failureRate = if (totalCalls == 0L) 0.0 else failedCalls.toDouble() / totalCalls.toDouble(),
                // 평균을 저장하지 않고 합계/건수로 복원한다. 상위 롤업에서도 값이 틀어지지 않는다.
                averageDurationMs = if (totalCalls == 0L) 0.0 else totalDurationMs.toDouble() / totalCalls.toDouble(),
                maxDurationMs = rs.getLong("max_duration_ms"),
            )
        }
    }

    override fun topEndpoints(from: Instant, to: Instant, limit: Int): List<TopEndpoint> {
        return jdbcTemplate.query(
            """
            SELECT method, path_pattern, count(*) AS total_calls
            FROM api_call_events
            WHERE occurred_at >= :from AND occurred_at < :to
            GROUP BY method, path_pattern
            ORDER BY total_calls DESC, method, path_pattern
            LIMIT :limit
            """.trimIndent(),
            params(from, to).addValue("limit", limit),
        ) { rs, _ ->
            TopEndpoint(rs.getString("method"), rs.getString("path_pattern"), rs.getLong("total_calls"))
        }
    }

    override fun clients(from: Instant, to: Instant): List<ClientCall> {
        return jdbcTemplate.query(
            """
            SELECT coalesce(api_client_name, 'anonymous') AS api_client_name, count(*) AS total_calls
            FROM api_call_events
            WHERE occurred_at >= :from AND occurred_at < :to
            GROUP BY coalesce(api_client_name, 'anonymous')
            ORDER BY total_calls DESC, api_client_name
            """.trimIndent(),
            params(from, to),
        ) { rs, _ ->
            ClientCall(rs.getString("api_client_name"), rs.getLong("total_calls"))
        }
    }

    override fun authFailures(from: Instant, to: Instant): List<AuthFailure> {
        return jdbcTemplate.query(
            """
            SELECT auth_result, count(*) AS total_calls
            FROM api_call_events
            WHERE occurred_at >= :from
              AND occurred_at < :to
              AND auth_result IN ('MISSING_API_KEY', 'INVALID_API_KEY', 'FORBIDDEN')
            GROUP BY auth_result
            ORDER BY total_calls DESC, auth_result
            """.trimIndent(),
            params(from, to),
        ) { rs, _ ->
            AuthFailure(rs.getString("auth_result"), rs.getLong("total_calls"))
        }
    }

    private fun params(bucket: String, from: Instant, to: Instant): MapSqlParameterSource {
        return params(from, to).addValue("bucket", bucket)
    }

    private fun params(from: Instant, to: Instant): MapSqlParameterSource {
        return MapSqlParameterSource()
            .addValue("from", Timestamp.from(from))
            .addValue("to", Timestamp.from(to))
    }
}
