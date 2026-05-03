package com.example.gateway.security

import com.example.internalauth.InternalAuthHeaders
import com.example.internalauth.InternalAuthPayload
import com.example.internalauth.InternalAuthSigner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpCookie
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64

class InternalAuthGatewayFilterFactoryTests {

    private val fixedInstant = Instant.parse("2026-03-09T00:00:00Z")
    private val signer = InternalAuthSigner(SECRET)
    private val factory = InternalAuthGatewayFilterFactory(signer).also {
        it.clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    }

    @Test
    fun `gateway filter adds signed internal auth headers`() {
        var capturedAppId: String? = null
        var capturedSignature: String? = null
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest
                .get("/api/me")
                .cookie(HttpCookie("APP1SESSION", "session-1"))
                .header(InternalAuthHeaders.SIGNATURE, "attacker-signature")
                .build(),
        )

        factory.apply(
            InternalAuthGatewayFilterFactory.Config(
                appId = "app1",
                sessionCookieName = "APP1SESSION",
            ),
        ).filter(exchange, GatewayFilterChain { mutatedExchange ->
            val headers = mutatedExchange.request.headers
            capturedAppId = headers.getFirst(InternalAuthHeaders.APP_ID)
            capturedSignature = headers.getFirst(InternalAuthHeaders.SIGNATURE)

            val payload = InternalAuthPayload(
                appId = "app1",
                method = "GET",
                path = "/api/me",
                sessionId = "session-1",
                issuedAtEpochSeconds = fixedInstant.epochSecond,
            )
            assertTrue(signer.verify(payload, capturedSignature!!))
            Mono.empty()
        }).block()

        assertEquals("app1", capturedAppId)
        assertNotEquals("attacker-signature", capturedSignature)
    }

    @Test
    fun `gateway filter signs backend path after route prefix is removed`() {
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest
                .get("/app1/api/me")
                .cookie(HttpCookie("APP1SESSION", "session-1"))
                .build(),
        )

        factory.apply(
            InternalAuthGatewayFilterFactory.Config(
                appId = "app1",
                sessionCookieName = "APP1SESSION",
            ),
        ).filter(exchange, GatewayFilterChain { mutatedExchange ->
            val headers = mutatedExchange.request.headers
            val signature = headers.getFirst(InternalAuthHeaders.SIGNATURE)!!

            val payload = InternalAuthPayload(
                appId = "app1",
                method = "GET",
                path = "/api/me",
                sessionId = "session-1",
                issuedAtEpochSeconds = fixedInstant.epochSecond,
            )
            assertTrue(signer.verify(payload, signature))
            Mono.empty()
        }).block()
    }

    @Test
    fun `gateway filter decodes spring session cookie before signing`() {
        val sessionId = "93bfec43-2f0f-4dba-bedd-701a1de445f0"
        val encodedSessionId = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(sessionId.toByteArray(StandardCharsets.UTF_8))
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest
                .get("/app1/api/me")
                .cookie(HttpCookie("APP1SESSION", encodedSessionId))
                .build(),
        )

        factory.apply(
            InternalAuthGatewayFilterFactory.Config(
                appId = "app1",
                sessionCookieName = "APP1SESSION",
            ),
        ).filter(exchange, GatewayFilterChain { mutatedExchange ->
            val signature = mutatedExchange.request.headers.getFirst(InternalAuthHeaders.SIGNATURE)!!
            val payload = InternalAuthPayload(
                appId = "app1",
                method = "GET",
                path = "/api/me",
                sessionId = sessionId,
                issuedAtEpochSeconds = fixedInstant.epochSecond,
            )

            assertTrue(signer.verify(payload, signature))
            Mono.empty()
        }).block()
    }

    private companion object {
        private const val SECRET = "test-secret"
    }
}
