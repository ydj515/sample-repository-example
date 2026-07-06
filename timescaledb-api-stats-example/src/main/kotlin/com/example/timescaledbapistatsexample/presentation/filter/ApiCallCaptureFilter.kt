package com.example.timescaledbapistatsexample.presentation.filter

import com.example.timescaledbapistatsexample.application.ApiCallEventPublisher
import com.example.timescaledbapistatsexample.domain.model.ApiAuthResult
import com.example.timescaledbapistatsexample.domain.model.ApiCallEvent
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.time.Instant
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerMapping

class ApiCallCaptureFilter(
    private val publisher: ApiCallEventPublisher,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        // context path가 설정돼도 앱 내부 경로로 매칭되도록 servletPath를 사용한다.
        return RequestFilterExclusions.isExcluded(request.servletPath)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val occurredAt = Instant.now()
        val startedAt = System.nanoTime()
        var errorType: String? = null

        try {
            filterChain.doFilter(request, response)
        } catch (ex: Exception) {
            errorType = ex.javaClass.simpleName
            response.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            throw ex
        } finally {
            val durationMs = (System.nanoTime() - startedAt) / 1_000_000
            publisher.publish(
                ApiCallEvent(
                    occurredAt = occurredAt,
                    apiClientId = request.getAttribute(ApiKeyAuthFilter.ATTR_API_CLIENT_ID) as? Long,
                    apiClientName = request.getAttribute(ApiKeyAuthFilter.ATTR_API_CLIENT_NAME) as? String,
                    authResult = request.getAttribute(ApiKeyAuthFilter.ATTR_AUTH_RESULT) as? String ?: ApiAuthResult.ALLOWED.name,
                    deniedReason = request.getAttribute(ApiKeyAuthFilter.ATTR_DENIED_REASON) as? String,
                    method = request.method,
                    path = request.servletPath,
                    pathPattern = resolvePathPattern(request),
                    status = response.status,
                    durationMs = durationMs,
                    clientIp = request.getHeader("X-Forwarded-For")?.takeIf { it.isNotBlank() }?.substringBefore(",")?.trim() ?: request.remoteAddr,
                    userAgent = request.getHeader("User-Agent"),
                    errorType = errorType,
                ),
            )
        }
    }

    private fun resolvePathPattern(request: HttpServletRequest): String {
        return request.getAttribute(ApiKeyAuthFilter.ATTR_ROUTE_PATTERN) as? String
            ?: request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as? String
            ?: request.servletPath
    }
}
