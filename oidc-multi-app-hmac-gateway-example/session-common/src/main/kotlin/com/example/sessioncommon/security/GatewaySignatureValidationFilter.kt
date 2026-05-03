package com.example.sessioncommon.security

import com.example.internalauth.InternalAuthHeaders
import com.example.internalauth.InternalAuthPayload
import com.example.internalauth.InternalAuthSigner
import com.example.sessioncommon.config.SessionPolicyProperties
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Clock
import java.time.Instant

class GatewaySignatureValidationFilter(
    private val sessionPolicyProperties: SessionPolicyProperties,
    private val clock: Clock = Clock.systemUTC(),
) : OncePerRequestFilter() {

    private val pathMatcher = AntPathMatcher()
    private val signer = InternalAuthSigner(sessionPolicyProperties.internalAuth.secret)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        if (!sessionPolicyProperties.internalAuth.enabled) {
            return true
        }

        val protectedPath = sessionPolicyProperties.internalAuth.protectedPathPatterns
            .any { pattern -> pathMatcher.match(pattern, request.requestURI) }
        if (!protectedPath) {
            return true
        }

        return !sessionPolicyProperties.internalAuth.required && !hasAnyInternalAuthHeader(request)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val verificationError = verify(request)
        if (verificationError != null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, verificationError)
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun verify(request: HttpServletRequest): String? {
        val appId = request.getHeader(InternalAuthHeaders.APP_ID)
            ?: return "missing gateway signature app id"
        val method = request.getHeader(InternalAuthHeaders.METHOD)
            ?: return "missing gateway signature method"
        val path = request.getHeader(InternalAuthHeaders.PATH)
            ?: return "missing gateway signature path"
        val sessionId = request.getHeader(InternalAuthHeaders.SESSION_ID)
            ?: return "missing gateway signature session id"
        val issuedAt = request.getHeader(InternalAuthHeaders.ISSUED_AT)?.toLongOrNull()
            ?: return "missing gateway signature issued-at"
        val signature = request.getHeader(InternalAuthHeaders.SIGNATURE)
            ?: return "missing gateway signature"

        if (appId != sessionPolicyProperties.appId) {
            return "gateway signature app id mismatch"
        }
        if (!method.equals(request.method, ignoreCase = true)) {
            return "gateway signature method mismatch"
        }
        if (path != request.requestURI) {
            return "gateway signature path mismatch"
        }

        val currentSessionId = request.getSession(false)?.id
            ?: return "session required for gateway signature validation"
        if (sessionId != currentSessionId) {
            return "gateway signature session mismatch"
        }

        val now = Instant.now(clock).epochSecond
        val maxAgeSeconds = sessionPolicyProperties.internalAuth.maxAge.seconds
        if (issuedAt > now + CLOCK_SKEW_SECONDS || now - issuedAt > maxAgeSeconds + CLOCK_SKEW_SECONDS) {
            return "gateway signature expired"
        }

        val payload = InternalAuthPayload(
            appId = appId,
            method = method,
            path = path,
            sessionId = sessionId,
            issuedAtEpochSeconds = issuedAt,
        )
        if (!signer.verify(payload, signature)) {
            return "invalid gateway signature"
        }

        return null
    }

    private fun hasAnyInternalAuthHeader(request: HttpServletRequest): Boolean {
        return InternalAuthHeaders.all.any { header -> request.getHeader(header) != null }
    }

    private companion object {
        private const val CLOCK_SKEW_SECONDS = 5
    }
}
