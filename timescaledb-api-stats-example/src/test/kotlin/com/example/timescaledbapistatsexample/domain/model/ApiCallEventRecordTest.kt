package com.example.timescaledbapistatsexample.domain.model

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiCallEventRecordTest {
    @Test
    fun `Redis Stream fields를 저장 record로 변환한다`() {
        val record = ApiCallEventRecord.from(
            streamId = "1710000000000-0",
            fields = mapOf(
                "occurredAt" to "2026-07-07T00:00:00Z",
                "apiClientId" to "1",
                "apiClientName" to "demo-client-01",
                "authResult" to "ALLOWED",
                "deniedReason" to "",
                "method" to "GET",
                "path" to "/api/products/1",
                "pathPattern" to "/api/products/{id}",
                "status" to "200",
                "durationMs" to "12",
                "clientIp" to "127.0.0.1",
                "userAgent" to "k6",
                "errorType" to "",
            ),
        )

        assertEquals("1710000000000-0", record.streamId)
        assertEquals(Instant.parse("2026-07-07T00:00:00Z"), record.occurredAt)
        assertEquals(1, record.apiClientId)
        assertEquals(null, record.deniedReason)
    }
}
