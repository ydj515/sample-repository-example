package com.example.timescaledbapistatsexample.infrastructure.persistence

import com.example.timescaledbapistatsexample.domain.model.ApiCallEventRecord
import com.example.timescaledbapistatsexample.domain.port.ApiCallEventStore
import java.sql.Timestamp
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class JdbcApiCallEventStore(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : ApiCallEventStore {
    override fun write(records: List<ApiCallEventRecord>) {
        if (records.isEmpty()) return

        val sql = """
            INSERT INTO api_call_events (
                stream_id,
                occurred_at,
                api_client_id,
                api_client_name,
                auth_result,
                denied_reason,
                method,
                path,
                path_pattern,
                status,
                duration_ms,
                client_ip,
                user_agent,
                error_type
            )
            VALUES (
                :streamId,
                :occurredAt,
                :apiClientId,
                :apiClientName,
                :authResult,
                :deniedReason,
                :method,
                :path,
                :pathPattern,
                :status,
                :durationMs,
                :clientIp,
                :userAgent,
                :errorType
            )
            ON CONFLICT (stream_id, occurred_at) DO NOTHING
        """.trimIndent()

        jdbcTemplate.batchUpdate(
            sql,
            records.map { record ->
                MapSqlParameterSource()
                    .addValue("streamId", record.streamId)
                    .addValue("occurredAt", Timestamp.from(record.occurredAt))
                    .addValue("apiClientId", record.apiClientId)
                    .addValue("apiClientName", record.apiClientName)
                    .addValue("authResult", record.authResult)
                    .addValue("deniedReason", record.deniedReason)
                    .addValue("method", record.method)
                    .addValue("path", record.path)
                    .addValue("pathPattern", record.pathPattern)
                    .addValue("status", record.status)
                    .addValue("durationMs", record.durationMs)
                    .addValue("clientIp", record.clientIp)
                    .addValue("userAgent", record.userAgent)
                    .addValue("errorType", record.errorType)
            }.toTypedArray(),
        )
    }
}
