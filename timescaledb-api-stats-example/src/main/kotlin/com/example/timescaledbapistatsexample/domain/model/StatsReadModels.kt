package com.example.timescaledbapistatsexample.domain.model

import java.time.Instant

/**
 * 통계 조회 결과의 도메인 읽기모델.
 * API 응답 형태(presentation의 *Response DTO)와 분리해, API 계약이 바뀌어도 domain은 영향받지 않게 한다.
 */
data class BucketCount(
    val bucket: Instant,
    val totalCalls: Long,
)

data class BucketLatency(
    val bucket: Instant,
    val averageDurationMs: Double,
    val maxDurationMs: Long,
)

data class BucketFailureRate(
    val bucket: Instant,
    val totalCalls: Long,
    val failedCalls: Long,
    val failureRate: Double,
)

data class TopEndpoint(
    val method: String,
    val pathPattern: String,
    val totalCalls: Long,
)

data class ClientCall(
    val apiClientName: String,
    val totalCalls: Long,
)

data class AuthFailure(
    val authResult: String,
    val totalCalls: Long,
)
