package com.example.internalauth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InternalAuthSignerTests {

    private val signer = InternalAuthSigner("test-secret")

    @Test
    fun `signer verifies the same payload`() {
        val payload = InternalAuthPayload(
            appId = "app1",
            method = "GET",
            path = "/api/me",
            sessionId = "session-1",
            issuedAtEpochSeconds = 1_700_000_000,
        )

        val signature = signer.sign(payload)

        assertTrue(signer.verify(payload, signature))
    }

    @Test
    fun `signer rejects tampered payload`() {
        val payload = InternalAuthPayload(
            appId = "app1",
            method = "GET",
            path = "/api/me",
            sessionId = "session-1",
            issuedAtEpochSeconds = 1_700_000_000,
        )
        val tampered = payload.copy(path = "/api/admin/users/alice/logout-all")

        val signature = signer.sign(payload)

        assertFalse(signer.verify(tampered, signature))
    }
}
