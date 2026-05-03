package com.example.sessioncommon.security

import com.example.sessioncommon.config.SessionPolicyProperties
import com.example.sessioncommon.session.SessionLookupService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Duration

class SessionRevalidationInterceptor(
    private val sessionPolicyProperties: SessionPolicyProperties,
    private val sessionLookupService: SessionLookupService,
    private val tierResolver: ApiSecurityTierResolver,
) : org.springframework.web.servlet.HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication ?: return true
        if (!authentication.isAuthenticated || authentication is AnonymousAuthenticationToken) {
            return true
        }

        val sessionId = request.getSession(false)?.id
        if (sessionId.isNullOrBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "세션이 존재하지 않습니다.")
            return false
        }

        val ttl = when (tierResolver.resolve(handler)) {
            ApiSecurityLevel.P0_CRITICAL -> Duration.ZERO
            ApiSecurityLevel.P1_SENSITIVE -> sessionPolicyProperties.revalidation.sensitiveTtl
            ApiSecurityLevel.P2_STANDARD -> sessionPolicyProperties.revalidation.standardTtl
        }

        val valid = sessionLookupService.revalidateSession(
            sessionId = sessionId,
            principalName = authentication.name,
            cacheTtl = ttl,
        )

        if (!valid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "세션이 더 이상 유효하지 않습니다.")
            return false
        }

        return true
    }
}
