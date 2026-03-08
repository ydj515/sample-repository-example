package com.example.oidccommon.security

import com.example.oidccommon.config.AppSecurityProperties
import com.example.oidccommon.session.SessionAttributeNames
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class SessionAppTaggingFilter(
    private val appSecurityProperties: AppSecurityProperties,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
        val session = request.getSession(false)

        if (session != null &&
            authentication != null &&
            authentication.isAuthenticated &&
            authentication !is AnonymousAuthenticationToken &&
            session.getAttribute(SessionAttributeNames.APP_ID) != appSecurityProperties.appId
        ) {
            session.setAttribute(SessionAttributeNames.APP_ID, appSecurityProperties.appId)
        }

        filterChain.doFilter(request, response)
    }
}
