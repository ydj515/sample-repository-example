package com.example.internalauth

data class InternalAuthPayload(
    val appId: String,
    val method: String,
    val path: String,
    val sessionId: String,
    val issuedAtEpochSeconds: Long,
) {
    fun canonicalValue(): String {
        return listOf(
            appId,
            method.uppercase(),
            path,
            sessionId,
            issuedAtEpochSeconds.toString(),
        ).joinToString("\n")
    }
}
