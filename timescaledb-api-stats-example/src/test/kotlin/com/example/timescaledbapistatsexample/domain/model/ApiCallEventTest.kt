package com.example.timescaledbapistatsexample.domain.model

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiCallEventTest {
    @Test
    fun `이벤트를 Redis Stream 필드 map으로 변환한다`() {
        val event = ApiCallEvent(
            occurredAt = Instant.parse("2026-07-07T00:00:00Z"),
            apiClientId = 1,
            apiClientName = "demo-client-01",
            authResult = "ALLOWED",
            deniedReason = null,
            method = "GET",
            path = "/api/products/1",
            pathPattern = "/api/products/{id}",
            status = 200,
            durationMs = 12,
            clientIp = "127.0.0.1",
            userAgent = "k6",
            errorType = null,
        )

        val fields = event.toStreamFields()

        assertEquals("2026-07-07T00:00:00Z", fields["occurredAt"])
        assertEquals("1", fields["apiClientId"])
        assertEquals("ALLOWED", fields["authResult"])
        assertEquals("", fields["deniedReason"])
    }
}
