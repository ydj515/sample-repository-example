package com.example.timescaledbapistatsexample.presentation

import com.example.timescaledbapistatsexample.domain.model.StatsPeriod
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
    ): List<BucketCountResponse> = reader.calls(bucket, from, to).map { it.toResponse() }

    @GetMapping("/latency")
    fun latency(
        @RequestParam(defaultValue = "1 minute") bucket: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant,
    ): List<BucketLatencyResponse> = reader.latency(bucket, from, to).map { it.toResponse() }

    @GetMapping("/failure-rate")
    fun failureRate(
        @RequestParam(defaultValue = "1 minute") bucket: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant,
    ): List<BucketFailureRateResponse> = reader.failureRate(bucket, from, to).map { it.toResponse() }

    @GetMapping("/api-key-calls")
    fun apiKeyCalls(
        @RequestParam(defaultValue = "day") period: String = "day",
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant,
        @RequestParam(required = false) apiClientId: Long? = null,
        @RequestParam(required = false) method: String? = null,
        @RequestParam(required = false) pathPattern: String? = null,
        @RequestParam(defaultValue = "100") limit: Int = 100,
    ): List<ApiKeyCallStatResponse> {
        val statsPeriod = StatsPeriod.from(period)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "period must be day, month, or year")

        return reader.apiKeyCalls(
            period = statsPeriod,
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
}
