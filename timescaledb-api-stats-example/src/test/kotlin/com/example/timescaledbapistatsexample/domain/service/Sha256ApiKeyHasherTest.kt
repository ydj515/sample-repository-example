package com.example.timescaledbapistatsexample.domain.service

import kotlin.test.Test
import kotlin.test.assertEquals

class Sha256ApiKeyHasherTest {
    @Test
    fun `demo key를 SHA-256 hex로 변환한다`() {
        val hash = Sha256ApiKeyHasher.hash("demo-key-client-01")

        assertEquals(
            "7ef01915c9607d617849c617d2700193cdd1e77e03c89055debe57fc4ec1b47f",
            hash,
        )
    }
}
