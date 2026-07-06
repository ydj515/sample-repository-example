package com.example.timescaledbapistatsexample.application

import com.example.timescaledbapistatsexample.domain.model.StreamEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class StreamEntryParserTest {
    private fun validFields(): Map<String, String> = mapOf(
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
    )

    @Test
    fun `정상 엔트리는 record와 validId로 수집한다`() {
        val parsed = StreamEntryParser.parse(
            listOf(StreamEntry(id = "1-0", fields = validFields())),
        )

        assertEquals(1, parsed.records.size)
        assertEquals(listOf("1-0"), parsed.validIds)
        assertEquals(emptyList(), parsed.invalidIds)
    }

    @Test
    fun `변환 불가 엔트리 한 건이 있어도 정상 엔트리는 저장 대상으로 남는다`() {
        val parsed = StreamEntryParser.parse(
            listOf(
                StreamEntry(id = "1-0", fields = validFields()),
                StreamEntry(id = "2-0", fields = mapOf("method" to "GET")), // 필수 필드 누락
            ),
        )

        assertEquals(1, parsed.records.size)
        assertEquals(listOf("1-0"), parsed.validIds)
        assertEquals(listOf("2-0"), parsed.invalidIds)
    }
}
