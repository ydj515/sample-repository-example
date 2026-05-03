package com.example.sessioncommon.security

import com.example.internalauth.InternalAuthHeaders
import com.example.internalauth.InternalAuthPayload
import com.example.internalauth.InternalAuthSigner
import com.example.sessioncommon.config.InternalAuthProperties
import com.example.sessioncommon.config.SessionPolicyProperties
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class GatewaySignatureValidationFilterTests {

    private val fixedInstant = Instant.parse("2026-03-09T00:00:00Z")
    private val clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    private val signer = InternalAuthSigner(SECRET)
    private val filter = GatewaySignatureValidationFilter(
        SessionPolicyProperties(
            appId = "app1",
            internalAuth = InternalAuthProperties(
                enabled = true,
                required = true,
                secret = SECRET,
                maxAge = Duration.ofSeconds(30),
            ),
        ),
        clock,
    )

    @Test
    fun `valid gateway signature passes the request`() {
        val request = signedRequest()
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertEquals(HttpServletResponse.SC_OK, response.status)
        assertEquals("/api/me", (chain.request as MockHttpServletRequest).requestURI)
    }

    @Test
    fun `missing gateway signature is rejected when required`() {
        val request = MockHttpServletRequest("GET", "/api/me").apply {
            setSession(MockHttpSession(null, "session-1").apply {
                setAttribute("sample", "value")
            })
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status)
    }

    @Test
    fun `tampered path is rejected`() {
        val request = signedRequest(path = "/api/sensitive").apply {
            setRequestURI("/api/me")
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status)
    }

    @Test
    fun `unsigned request passes when signature is optional`() {
        val optionalFilter = GatewaySignatureValidationFilter(
            SessionPolicyProperties(
                appId = "app1",
                internalAuth = InternalAuthProperties(
                    enabled = true,
                    required = false,
                    secret = SECRET,
                ),
            ),
            clock,
        )
        val request = MockHttpServletRequest("GET", "/api/me")
        val response = MockHttpServletResponse()

        optionalFilter.doFilter(request, response, MockFilterChain())

        assertEquals(HttpServletResponse.SC_OK, response.status)
        assertNull(response.errorMessage)
    }

    private fun signedRequest(
        path: String = "/api/me",
    ): MockHttpServletRequest {
        val issuedAt = fixedInstant.epochSecond
        val payload = InternalAuthPayload(
            appId = "app1",
            method = "GET",
            path = path,
            sessionId = "session-1",
            issuedAtEpochSeconds = issuedAt,
        )

        return MockHttpServletRequest("GET", path).apply {
            setSession(MockHttpSession(null, "session-1").apply {
                setAttribute("sample", "value")
            })
            addHeader(InternalAuthHeaders.APP_ID, payload.appId)
            addHeader(InternalAuthHeaders.METHOD, payload.method)
            addHeader(InternalAuthHeaders.PATH, payload.path)
            addHeader(InternalAuthHeaders.SESSION_ID, payload.sessionId)
            addHeader(InternalAuthHeaders.ISSUED_AT, payload.issuedAtEpochSeconds.toString())
            addHeader(InternalAuthHeaders.SIGNATURE, signer.sign(payload))
        }
    }

    private companion object {
        private const val SECRET = "test-secret"
    }
}
