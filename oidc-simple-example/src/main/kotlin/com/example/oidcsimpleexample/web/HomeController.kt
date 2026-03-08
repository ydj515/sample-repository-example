package com.example.oidcsimpleexample.web

import com.example.oidcsimpleexample.service.SessionLookupService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController(
    private val sessionLookupService: SessionLookupService,
) {

    @GetMapping("/")
    fun index(
        authentication: Authentication?,
        request: HttpServletRequest,
        model: Model,
    ): String {
        val oidcUser = authentication?.principal as? OidcUser
        model.addAttribute("authenticated", authentication?.isAuthenticated == true && oidcUser != null)
        model.addAttribute("username", oidcUser?.preferredUsername ?: authentication?.name ?: "anonymous")
        model.addAttribute("authorities", authentication?.authorities?.map { it.authority }.orEmpty())
        model.addAttribute("claims", oidcUser?.claims.orEmpty())
        model.addAttribute("sessionId", request.getSession(false)?.id)
        model.addAttribute(
            "activeSessions",
            authentication?.name?.let(sessionLookupService::findUserSessions).orEmpty(),
        )
        return "index"
    }
}
