package com.example.oidcsimpleexample.security

import com.example.oidcsimpleexample.config.AppSecurityProperties
import com.example.oidcsimpleexample.service.SessionLookupService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.time.Duration

@Component
class SessionRevalidationInterceptor(
    private val appSecurityProperties: AppSecurityProperties,
    private val sessionLookupService: SessionLookupService,
    private val tierResolver: ApiSecurityTierResolver,
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication ?: return true
        if (!authentication.isAuthenticated) {
            return true
        }

        val sessionId = request.getSession(false)?.id
        if (sessionId.isNullOrBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "세션이 존재하지 않습니다.")
            return false
        }

        val ttl = when (tierResolver.resolve(handler)) {
            ApiSecurityLevel.P0_CRITICAL -> Duration.ZERO
            ApiSecurityLevel.P1_SENSITIVE -> appSecurityProperties.revalidation.sensitiveTtl
            ApiSecurityLevel.P2_STANDARD -> appSecurityProperties.revalidation.standardTtl
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
