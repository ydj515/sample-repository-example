package com.example.sessioncommon.security

import com.example.sessioncommon.config.SessionPolicyProperties
import com.example.sessioncommon.session.SessionAttributeNames
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class SessionAppTaggingFilter(
    private val sessionPolicyProperties: SessionPolicyProperties,
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
            session.getAttribute(SessionAttributeNames.APP_ID) != sessionPolicyProperties.appId
        ) {
            session.setAttribute(SessionAttributeNames.APP_ID, sessionPolicyProperties.appId)
        }

        filterChain.doFilter(request, response)
    }
}
