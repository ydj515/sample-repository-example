package com.example.timescaledbapistatsexample.presentation.response

import com.example.timescaledbapistatsexample.domain.model.AuthFailure
import com.example.timescaledbapistatsexample.domain.model.ApiKeyCallStat
import com.example.timescaledbapistatsexample.domain.model.BucketCount
import com.example.timescaledbapistatsexample.domain.model.BucketFailureRate
import com.example.timescaledbapistatsexample.domain.model.BucketLatency
import com.example.timescaledbapistatsexample.domain.model.ClientCall
import com.example.timescaledbapistatsexample.domain.model.TopEndpoint
import java.time.Instant

/**
 * 통계 API 응답 DTO. presentation 계층의 API 계약이며, domain 읽기모델과 독립적으로 진화한다.
 */
data class BucketCountResponse(
    val bucket: Instant,
    val totalCalls: Long,
)

data class BucketLatencyResponse(
    val bucket: Instant,
    val averageDurationMs: Double,
    val maxDurationMs: Long,
)

data class BucketFailureRateResponse(
    val bucket: Instant,
    val totalCalls: Long,
    val failedCalls: Long,
    val failureRate: Double,
)

data class ApiKeyCallStatResponse(
    val bucket: Instant,
    val apiClientId: Long?,
    val apiClientName: String,
    val method: String,
    val pathPattern: String,
    val totalCalls: Long,
    val failedCalls: Long,
    val failureRate: Double,
    val averageDurationMs: Double,
    val maxDurationMs: Long,
)

data class TopEndpointResponse(
    val method: String,
    val pathPattern: String,
    val totalCalls: Long,
)

data class ClientCallResponse(
    val apiClientName: String,
    val totalCalls: Long,
)

data class AuthFailureResponse(
    val authResult: String,
    val totalCalls: Long,
)

fun BucketCount.toResponse() = BucketCountResponse(bucket, totalCalls)

fun BucketLatency.toResponse() = BucketLatencyResponse(bucket, averageDurationMs, maxDurationMs)

fun BucketFailureRate.toResponse() = BucketFailureRateResponse(bucket, totalCalls, failedCalls, failureRate)

fun ApiKeyCallStat.toResponse() = ApiKeyCallStatResponse(
    bucket = bucket,
    apiClientId = apiClientId,
    apiClientName = apiClientName,
    method = method,
    pathPattern = pathPattern,
    totalCalls = totalCalls,
    failedCalls = failedCalls,
    failureRate = failureRate,
    averageDurationMs = averageDurationMs,
    maxDurationMs = maxDurationMs,
)

fun TopEndpoint.toResponse() = TopEndpointResponse(method, pathPattern, totalCalls)

fun ClientCall.toResponse() = ClientCallResponse(apiClientName, totalCalls)

fun AuthFailure.toResponse() = AuthFailureResponse(authResult, totalCalls)
