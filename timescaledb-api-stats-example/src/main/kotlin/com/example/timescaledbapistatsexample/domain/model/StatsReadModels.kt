package com.example.timescaledbapistatsexample.domain.model

import java.time.Instant

/**
 * 통계 조회 결과의 도메인 읽기모델.
 * API 응답 형태(presentation의 *Response DTO)와 분리해, API 계약이 바뀌어도 domain은 영향받지 않게 한다.
 */
/**
 * 집계 기간. 기간마다 대응하는 continuous aggregate view가 하나씩 있다.
 *
 * [aggregateView]는 이 enum에 하드코딩된 상수이므로 SQL에 직접 넣어도 사용자 입력이 섞이지 않는다.
 */
enum class StatsPeriod(
    val queryValue: String,
    val bucket: String,
    val aggregateView: String,
) {
    DAY("day", "1 day", "api_key_call_stats_daily"),
    MONTH("month", "1 month", "api_key_call_stats_monthly"),
    YEAR("year", "1 year", "api_key_call_stats_yearly"),
    ;

    companion object {
        fun from(value: String): StatsPeriod? {
            return entries.firstOrNull { it.queryValue == value.trim().lowercase() }
        }
    }
}

/**
 * 같은 통계를 어느 쪽에서 계산할지 고르는 스위치.
 *
 * - [RAW]: 조회 시점에 hypertable을 time_bucket으로 훑는다.
 *   배치 잡이나 온디맨드 쿼리로 직접 가공하는 방식과 같은 비용 구조다.
 * - [AGGREGATE]: 미리 말아둔 continuous aggregate를 읽는다.
 *   가공 책임을 TimescaleDB에 넘긴 방식이다.
 *
 * 두 값이 같은 응답 스키마를 돌려주므로 결과 일치 여부와 응답 시간을 그대로 비교할 수 있다.
 */
enum class StatsSource(
    val queryValue: String,
) {
    RAW("raw"),
    AGGREGATE("aggregate"),
    ;

    companion object {
        fun from(value: String): StatsSource? {
            return entries.firstOrNull { it.queryValue == value.trim().lowercase() }
        }
    }
}

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

data class ApiKeyCallStat(
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
