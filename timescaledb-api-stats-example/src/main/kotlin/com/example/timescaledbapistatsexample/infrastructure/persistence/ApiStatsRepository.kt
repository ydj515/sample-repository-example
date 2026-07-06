package com.example.timescaledbapistatsexample.infrastructure.persistence

import com.example.timescaledbapistatsexample.domain.model.AuthFailure
import com.example.timescaledbapistatsexample.domain.model.BucketCount
import com.example.timescaledbapistatsexample.domain.model.BucketFailureRate
import com.example.timescaledbapistatsexample.domain.model.BucketLatency
import com.example.timescaledbapistatsexample.domain.model.ClientCall
import com.example.timescaledbapistatsexample.domain.model.TopEndpoint
import com.example.timescaledbapistatsexample.domain.port.ApiStatsReader
import java.sql.Timestamp
import java.time.Instant
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
            BucketCount(rs.getTimestamp("bucket").toInstant(), rs.getLong("total_calls"))
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
                bucket = rs.getTimestamp("bucket").toInstant(),
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
                bucket = rs.getTimestamp("bucket").toInstant(),
                totalCalls = totalCalls,
                failedCalls = failedCalls,
                failureRate = if (totalCalls == 0L) 0.0 else failedCalls.toDouble() / totalCalls.toDouble(),
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
