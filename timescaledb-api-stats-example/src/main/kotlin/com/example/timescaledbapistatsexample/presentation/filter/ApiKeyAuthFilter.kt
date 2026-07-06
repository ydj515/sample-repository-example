package com.example.timescaledbapistatsexample.presentation.filter

import com.example.timescaledbapistatsexample.application.ApiAccessService
import com.example.timescaledbapistatsexample.domain.model.ApiAuthResult
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter

class ApiKeyAuthFilter(
    private val apiAccessService: ApiAccessService,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return RequestFilterExclusions.isExcluded(request.requestURI)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val decision = apiAccessService.authorize(
            apiKey = request.getHeader("X-API-Key"),
            method = request.method,
            path = request.requestURI,
        )

        request.setAttribute(ATTR_AUTH_RESULT, decision.authResult.name)
        request.setAttribute(ATTR_DENIED_REASON, decision.deniedReason)
        request.setAttribute(ATTR_ROUTE_PATTERN, decision.route?.pathPattern)
        request.setAttribute(ATTR_API_CLIENT_ID, decision.principal?.id)
        request.setAttribute(ATTR_API_CLIENT_NAME, decision.principal?.name)

        when (decision.authResult) {
            ApiAuthResult.ALLOWED -> filterChain.doFilter(request, response)
            ApiAuthResult.MISSING_API_KEY,
            ApiAuthResult.INVALID_API_KEY,
            -> writeError(response, HttpServletResponse.SC_UNAUTHORIZED, decision.authResult, decision.deniedReason)
            ApiAuthResult.FORBIDDEN -> writeError(
                response,
                HttpServletResponse.SC_FORBIDDEN,
                decision.authResult,
                decision.deniedReason,
            )
        }
    }

    private fun writeError(
        response: HttpServletResponse,
        status: Int,
        authResult: ApiAuthResult,
        message: String?,
    ) {
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write("""{"authResult":"${authResult.name}","message":"${message ?: "Access denied"}"}""")
    }

    companion object {
        const val ATTR_AUTH_RESULT = "apiStats.authResult"
        const val ATTR_DENIED_REASON = "apiStats.deniedReason"
        const val ATTR_ROUTE_PATTERN = "apiStats.routePattern"
        const val ATTR_API_CLIENT_ID = "apiStats.apiClientId"
        const val ATTR_API_CLIENT_NAME = "apiStats.apiClientName"
    }
}
