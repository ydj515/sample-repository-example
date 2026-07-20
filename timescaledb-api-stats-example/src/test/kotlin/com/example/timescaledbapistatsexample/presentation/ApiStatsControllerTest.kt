package com.example.timescaledbapistatsexample.presentation

import com.example.timescaledbapistatsexample.domain.model.AuthFailure
import com.example.timescaledbapistatsexample.domain.model.ApiKeyCallStat
import com.example.timescaledbapistatsexample.domain.model.BucketCount
import com.example.timescaledbapistatsexample.domain.model.BucketFailureRate
import com.example.timescaledbapistatsexample.domain.model.BucketLatency
import com.example.timescaledbapistatsexample.domain.model.ClientCall
import com.example.timescaledbapistatsexample.domain.model.StatsPeriod
import com.example.timescaledbapistatsexample.domain.model.StatsSource
import com.example.timescaledbapistatsexample.domain.model.TopEndpoint
import com.example.timescaledbapistatsexample.domain.port.ApiStatsReader
import org.springframework.mock.web.MockHttpServletResponse
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
        // 기본값은 continuous aggregate 조회다.
        assertEquals(StatsSource.AGGREGATE, request?.source)
        assertEquals(from, request?.from)
        assertEquals(to, request?.to)
        assertEquals(null, request?.apiClientId)
        assertEquals(null, request?.method)
        assertEquals(null, request?.pathPattern)
        assertEquals(100, request?.limit)
    }

    @Test
    fun `api key 호출 통계는 source=raw를 hypertable 조회로 전달한다`() {
        controller.apiKeyCalls(source = "raw", from = from, to = to)

        assertEquals(StatsSource.RAW, reader.lastApiKeyCallsRequest?.source)
    }

    @Test
    fun `api key 호출 통계는 지원하지 않는 source를 거부한다`() {
        val ex = assertFailsWith<ResponseStatusException> {
            controller.apiKeyCalls(source = "batch", from = from, to = to)
        }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    @Test
    fun `기간별로 대응하는 continuous aggregate view가 지정되어 있다`() {
        assertEquals("api_key_call_stats_daily", StatsPeriod.DAY.aggregateView)
        assertEquals("api_key_call_stats_monthly", StatsPeriod.MONTH.aggregateView)
        assertEquals("api_key_call_stats_yearly", StatsPeriod.YEAR.aggregateView)
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
    fun `결과가 전체 상한에 걸리면 잘림 헤더를 붙인다`() {
        reader.apiKeyCallsResult = List(ApiStatsReader.MAX_TOTAL_ROWS) { stat() }
        val response = MockHttpServletResponse()

        controller.apiKeyCalls(from = from, to = to, response = response)

        assertEquals("true", response.getHeader(ApiStatsController.HEADER_RESULT_TRUNCATED))
    }

    @Test
    fun `상한에 걸리지 않으면 잘림 헤더를 붙이지 않는다`() {
        reader.apiKeyCallsResult = listOf(stat())
        val response = MockHttpServletResponse()

        controller.apiKeyCalls(from = from, to = to, response = response)

        assertEquals(null, response.getHeader(ApiStatsController.HEADER_RESULT_TRUNCATED))
    }

    private fun stat() = ApiKeyCallStat(
        bucket = from,
        apiClientId = 1,
        apiClientName = "demo-client-01",
        method = "GET",
        pathPattern = "/api/products",
        totalCalls = 1,
        failedCalls = 0,
        failureRate = 0.0,
        averageDurationMs = 1.0,
        maxDurationMs = 1,
    )

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
            source: StatsSource,
            from: Instant,
            to: Instant,
            apiClientId: Long?,
            method: String?,
            pathPattern: String?,
            limit: Int,
        ): List<ApiKeyCallStat> {
            lastApiKeyCallsRequest =
                ApiKeyCallsRequest(period, source, from, to, apiClientId, method, pathPattern, limit)
            return apiKeyCallsResult
        }
    }

    private data class ApiKeyCallsRequest(
        val period: StatsPeriod,
        val source: StatsSource,
        val from: Instant,
        val to: Instant,
        val apiClientId: Long?,
        val method: String?,
        val pathPattern: String?,
        val limit: Int,
    )
}
