package com.example.timescaledbapistatsexample.presentation

import com.example.timescaledbapistatsexample.domain.model.StatsPeriod
import com.example.timescaledbapistatsexample.domain.model.StatsSource
import com.example.timescaledbapistatsexample.domain.port.ApiStatsReader
import com.example.timescaledbapistatsexample.presentation.response.ApiKeyCallStatResponse
import com.example.timescaledbapistatsexample.presentation.response.AuthFailureResponse
import com.example.timescaledbapistatsexample.presentation.response.BucketCountResponse
import com.example.timescaledbapistatsexample.presentation.response.BucketFailureRateResponse
import com.example.timescaledbapistatsexample.presentation.response.BucketLatencyResponse
import com.example.timescaledbapistatsexample.presentation.response.ClientCallResponse
import com.example.timescaledbapistatsexample.presentation.response.TopEndpointResponse
import com.example.timescaledbapistatsexample.presentation.response.toResponse
import java.time.Instant
import org.springframework.http.HttpStatus
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/stats")
class ApiStatsController(
    private val reader: ApiStatsReader,
) {
    @GetMapping("/calls")
    fun calls(
        @RequestParam(defaultValue = "1 minute") bucket: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant,
    ): List<BucketCountResponse> = reader.calls(bucket.toValidatedBucket(), from, to).map { it.toResponse() }

    @GetMapping("/latency")
    fun latency(
        @RequestParam(defaultValue = "1 minute") bucket: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant,
    ): List<BucketLatencyResponse> = reader.latency(bucket.toValidatedBucket(), from, to).map { it.toResponse() }

    @GetMapping("/failure-rate")
    fun failureRate(
        @RequestParam(defaultValue = "1 minute") bucket: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant,
    ): List<BucketFailureRateResponse> = reader.failureRate(bucket.toValidatedBucket(), from, to).map { it.toResponse() }

    /**
     * `source`로 같은 통계를 두 경로에서 뽑아 비교할 수 있다.
     *
     * - `source=aggregate`(기본): continuous aggregate 조회. 가공을 TimescaleDB에 위임한 결과.
     * - `source=raw`: hypertable을 조회 시점에 집계. 배치/온디맨드로 직접 가공하는 방식과 같은 비용.
     */
    @GetMapping("/api-key-calls")
    fun apiKeyCalls(
        @RequestParam(defaultValue = "day") period: String = "day",
        @RequestParam(defaultValue = "aggregate") source: String = "aggregate",
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant,
        @RequestParam(required = false) apiClientId: Long? = null,
        @RequestParam(required = false) method: String? = null,
        @RequestParam(required = false) pathPattern: String? = null,
        @RequestParam(defaultValue = "100") limit: Int = 100,
    ): List<ApiKeyCallStatResponse> {
        val statsPeriod = StatsPeriod.from(period)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "period must be day, month, or year")
        val statsSource = StatsSource.from(source)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "source must be raw or aggregate")

        return reader.apiKeyCalls(
            period = statsPeriod,
            source = statsSource,
            from = from,
            to = to,
            apiClientId = apiClientId,
            method = method.normalizedMethod(),
            pathPattern = pathPattern.blankToNull(),
            limit = limit.coerceIn(1, 500),
        ).map { it.toResponse() }
    }

    @GetMapping("/top-endpoints")
    fun topEndpoints(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant,
        @RequestParam(defaultValue = "10") limit: Int,
    ): List<TopEndpointResponse> = reader.topEndpoints(from, to, limit.coerceIn(1, 100)).map { it.toResponse() }

    @GetMapping("/clients")
    fun clients(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant,
    ): List<ClientCallResponse> = reader.clients(from, to).map { it.toResponse() }

    @GetMapping("/auth-failures")
    fun authFailures(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant,
    ): List<AuthFailureResponse> = reader.authFailures(from, to).map { it.toResponse() }

    private fun String?.blankToNull(): String? {
        return this?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun String?.normalizedMethod(): String? {
        return blankToNull()?.uppercase()
    }

    /**
     * `bucket`은 Postgres interval로 캐스팅된다.
     *
     * 파라미터 바인딩이라 SQL injection은 없지만, 검증 없이 넘기면 잘못된 문자열이
     * DB까지 내려가 PSQLException으로 500이 된다. 형식을 먼저 확인해 400으로 돌려준다.
     */
    private fun String.toValidatedBucket(): String {
        val normalized = trim()
        if (!BUCKET_PATTERN.matches(normalized)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "bucket must look like '<n> <unit>' (e.g. '1 minute', '5 minutes', '1 hour')",
            )
        }
        return normalized
    }

    companion object {
        private val BUCKET_PATTERN =
            Regex("^\\d+\\s+(second|minute|hour|day|week|month|year)s?$", RegexOption.IGNORE_CASE)
    }
}
