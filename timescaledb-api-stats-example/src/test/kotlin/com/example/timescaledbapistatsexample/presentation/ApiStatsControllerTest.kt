package com.example.timescaledbapistatsexample.presentation

import com.example.timescaledbapistatsexample.domain.model.AuthFailure
import com.example.timescaledbapistatsexample.domain.model.BucketCount
import com.example.timescaledbapistatsexample.domain.model.BucketFailureRate
import com.example.timescaledbapistatsexample.domain.model.BucketLatency
import com.example.timescaledbapistatsexample.domain.model.ClientCall
import com.example.timescaledbapistatsexample.domain.model.TopEndpoint
import com.example.timescaledbapistatsexample.domain.port.ApiStatsReader
import com.example.timescaledbapistatsexample.presentation.response.BucketCountResponse
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiStatsControllerTest {
    private val reader = RecordingStatsReader()
    private val controller = ApiStatsController(reader)
    private val from = Instant.parse("2026-07-07T00:00:00Z")
    private val to = Instant.parse("2026-07-07T01:00:00Z")

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

        override fun calls(bucket: String, from: Instant, to: Instant): List<BucketCount> = callsResult

        override fun latency(bucket: String, from: Instant, to: Instant): List<BucketLatency> = emptyList()

        override fun failureRate(bucket: String, from: Instant, to: Instant): List<BucketFailureRate> = emptyList()

        override fun topEndpoints(from: Instant, to: Instant, limit: Int): List<TopEndpoint> {
            lastTopEndpointLimit = limit
            return emptyList()
        }

        override fun clients(from: Instant, to: Instant): List<ClientCall> = emptyList()

        override fun authFailures(from: Instant, to: Instant): List<AuthFailure> = emptyList()
    }
}
