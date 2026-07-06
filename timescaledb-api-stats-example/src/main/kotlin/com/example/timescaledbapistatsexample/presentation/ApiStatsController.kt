package com.example.timescaledbapistatsexample.presentation

import com.example.timescaledbapistatsexample.domain.port.ApiStatsReader
import com.example.timescaledbapistatsexample.presentation.response.AuthFailureResponse
import com.example.timescaledbapistatsexample.presentation.response.BucketCountResponse
import com.example.timescaledbapistatsexample.presentation.response.BucketFailureRateResponse
import com.example.timescaledbapistatsexample.presentation.response.BucketLatencyResponse
import com.example.timescaledbapistatsexample.presentation.response.ClientCallResponse
import com.example.timescaledbapistatsexample.presentation.response.TopEndpointResponse
import com.example.timescaledbapistatsexample.presentation.response.toResponse
import java.time.Instant
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
}
