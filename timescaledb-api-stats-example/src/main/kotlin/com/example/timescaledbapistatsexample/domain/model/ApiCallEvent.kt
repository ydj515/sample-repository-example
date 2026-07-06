package com.example.timescaledbapistatsexample.domain.model

import java.time.Instant

data class ApiCallEvent(
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
)

fun ApiCallEvent.toStreamFields(): Map<String, String> {
    return mapOf(
        "occurredAt" to occurredAt.toString(),
        "apiClientId" to (apiClientId?.toString() ?: ""),
        "apiClientName" to (apiClientName ?: ""),
        "authResult" to authResult,
        "deniedReason" to (deniedReason ?: ""),
        "method" to method,
        "path" to path,
        "pathPattern" to pathPattern,
        "status" to status.toString(),
        "durationMs" to durationMs.toString(),
        "clientIp" to (clientIp ?: ""),
        "userAgent" to (userAgent ?: ""),
        "errorType" to (errorType ?: ""),
    )
}
