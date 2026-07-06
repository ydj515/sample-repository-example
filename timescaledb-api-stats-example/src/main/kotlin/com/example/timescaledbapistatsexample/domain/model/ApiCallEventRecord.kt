package com.example.timescaledbapistatsexample.domain.model

import java.time.Instant

data class ApiCallEventRecord(
    val streamId: String,
    val occurredAt: Instant,
    val apiClientId: Long?,
    val apiClientName: String?,
    val authResult: String,
    val deniedReason: String?,
    val method: String,
    val path: String,
    val pathPattern: String,
    val status: Int,
    val durationMs: Long,
    val clientIp: String?,
    val userAgent: String?,
    val errorType: String?,
) {
    companion object {
        fun from(streamId: String, fields: Map<String, String>): ApiCallEventRecord {
            return ApiCallEventRecord(
                streamId = streamId,
                occurredAt = Instant.parse(fields.require("occurredAt")),
                apiClientId = fields.optional("apiClientId")?.toLong(),
                apiClientName = fields.optional("apiClientName"),
                authResult = fields.require("authResult"),
                deniedReason = fields.optional("deniedReason"),
                method = fields.require("method"),
                path = fields.require("path"),
                pathPattern = fields.require("pathPattern"),
                status = fields.require("status").toInt(),
                durationMs = fields.require("durationMs").toLong(),
                clientIp = fields.optional("clientIp"),
                userAgent = fields.optional("userAgent"),
                errorType = fields.optional("errorType"),
            )
        }

        private fun Map<String, String>.require(key: String): String {
            return this[key] ?: error("Missing Redis Stream field: $key")
        }

        private fun Map<String, String>.optional(key: String): String? {
            return this[key]?.takeIf { it.isNotBlank() }
        }
    }
}
