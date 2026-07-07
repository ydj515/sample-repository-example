package com.example.timescaledbapistatsexample.presentation

import com.example.timescaledbapistatsexample.domain.model.AuthFailure
import com.example.timescaledbapistatsexample.domain.model.ApiKeyCallStat
import com.example.timescaledbapistatsexample.domain.model.BucketCount
import com.example.timescaledbapistatsexample.domain.model.BucketFailureRate
import com.example.timescaledbapistatsexample.domain.model.BucketLatency
import com.example.timescaledbapistatsexample.domain.model.ClientCall
import com.example.timescaledbapistatsexample.domain.model.StatsPeriod
import com.example.timescaledbapistatsexample.domain.model.TopEndpoint
import com.example.timescaledbapistatsexample.domain.port.ApiStatsReader
import com.example.timescaledbapistatsexample.presentation.response.ApiKeyCallStatResponse
import com.example.timescaledbapistatsexample.presentation.response.BucketCountResponse
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class ApiStatsControllerTest {
    private val reader = RecordingStatsReader()
    private val controller = ApiStatsController(reader)
    private val from = Instant.parse("2026-07-07T00:00:00Z")
    private val to = Instant.parse("2026-07-07T01:00:00Z")

    @Test
    fun `api key 호출 통계는 기본 day 기간과 기본 제한값을 전달한다`() {
        controller.apiKeyCalls(from = from, to = to)

        val request = reader.lastApiKeyCallsRequest
        assertEquals(StatsPeriod.DAY, request?.period)
        assertEquals(from, request?.from)
        assertEquals(to, request?.to)
        assertEquals(null, request?.apiClientId)
        assertEquals(null, request?.method)
        assertEquals(null, request?.pathPattern)
        assertEquals(100, request?.limit)
    }

    @Test
    fun `api key 호출 통계는 limit과 필터를 정리해서 전달한다`() {
        controller.apiKeyCalls(
            period = "month",
            from = from,
            to = to,
            apiClientId = 7,
            method = "get",
            pathPattern = "/api/products",
            limit = 999,
        )

        val request = reader.lastApiKeyCallsRequest
        assertEquals(StatsPeriod.MONTH, request?.period)
        assertEquals(7, request?.apiClientId)
        assertEquals("GET", request?.method)
        assertEquals("/api/products", request?.pathPattern)
        assertEquals(500, request?.limit)
    }

    @Test
    fun `api key 호출 통계는 지원하지 않는 기간을 거부한다`() {
        val ex = assertFailsWith<ResponseStatusException> {
            controller.apiKeyCalls(period = "week", from = from, to = to)
        }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    @Test
    fun `api key 호출 통계 읽기모델을 응답 DTO로 매핑한다`() {
        reader.apiKeyCallsResult = listOf(
            ApiKeyCallStat(
                bucket = from,
                apiClientId = 7,
                apiClientName = "demo-client-07",
                method = "GET",
                pathPattern = "/api/products",
                totalCalls = 11,
                failedCalls = 2,
                failureRate = 2.0 / 11.0,
                averageDurationMs = 12.5,
                maxDurationMs = 30,
            ),
        )

        val response = controller.apiKeyCalls(period = "year", from = from, to = to)

        assertEquals(1, response.size)
        assertEquals(
            ApiKeyCallStatResponse(
                bucket = from,
                apiClientId = 7,
                apiClientName = "demo-client-07",
                method = "GET",
                pathPattern = "/api/products",
                totalCalls = 11,
                failedCalls = 2,
                failureRate = 2.0 / 11.0,
                averageDurationMs = 12.5,
                maxDurationMs = 30,
            ),
            response.first(),
        )
    }

    @Test
    fun `top endpoint limit은 1 이상 100 이하로 제한한다`() {
        controller.topEndpoints(from = from, to = to, limit = 500)

        assertEquals(100, reader.lastTopEndpointLimit)
    }

    @Test
    fun `도메인 읽기모델을 응답 DTO로 매핑한다`() {
        reader.callsResult = listOf(BucketCount(bucket = from, totalCalls = 7))

        val response = controller.calls(bucket = "1 minute", from = from, to = to)

        assertEquals(1, response.size)
        assertEquals(BucketCountResponse(bucket = from, totalCalls = 7), response.first())
    }

    private class RecordingStatsReader : ApiStatsReader {
        var lastTopEndpointLimit: Int? = null
        var callsResult: List<BucketCount> = emptyList()
        var apiKeyCallsResult: List<ApiKeyCallStat> = emptyList()
        var lastApiKeyCallsRequest: ApiKeyCallsRequest? = null

        override fun calls(bucket: String, from: Instant, to: Instant): List<BucketCount> = callsResult

        override fun latency(bucket: String, from: Instant, to: Instant): List<BucketLatency> = emptyList()

        override fun failureRate(bucket: String, from: Instant, to: Instant): List<BucketFailureRate> = emptyList()

        override fun topEndpoints(from: Instant, to: Instant, limit: Int): List<TopEndpoint> {
            lastTopEndpointLimit = limit
            return emptyList()
        }

        override fun clients(from: Instant, to: Instant): List<ClientCall> = emptyList()

        override fun authFailures(from: Instant, to: Instant): List<AuthFailure> = emptyList()

        override fun apiKeyCalls(
            period: StatsPeriod,
            from: Instant,
            to: Instant,
            apiClientId: Long?,
            method: String?,
            pathPattern: String?,
            limit: Int,
        ): List<ApiKeyCallStat> {
            lastApiKeyCallsRequest = ApiKeyCallsRequest(period, from, to, apiClientId, method, pathPattern, limit)
            return apiKeyCallsResult
        }
    }

    private data class ApiKeyCallsRequest(
        val period: StatsPeriod,
        val from: Instant,
        val to: Instant,
        val apiClientId: Long?,
        val method: String?,
        val pathPattern: String?,
        val limit: Int,
    )
}
