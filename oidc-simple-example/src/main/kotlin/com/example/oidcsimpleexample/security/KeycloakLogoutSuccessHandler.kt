package com.example.oidcsimpleexample.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

class KeycloakLogoutSuccessHandler(
    private val endSessionUri: URI,
) : LogoutSuccessHandler {

    override fun onLogoutSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?,
    ) {
        val targetUrl = buildTargetUrl(request, authentication)
        response.sendRedirect(targetUrl)
    }

    private fun buildTargetUrl(
        request: HttpServletRequest,
        authentication: Authentication?,
    ): String {
        val baseUrlBuilder = UriComponentsBuilder
            .newInstance()
            .scheme(request.scheme)
            .host(request.serverName)

        if (request.serverPort != 80 && request.serverPort != 443) {
            baseUrlBuilder.port(request.serverPort)
        }

        val baseUrl = baseUrlBuilder
            .path("/")
            .build()
            .toUriString()

        val builder = UriComponentsBuilder
            .fromUri(endSessionUri)
            .queryParam("post_logout_redirect_uri", baseUrl)

        val idToken = (authentication as? OAuth2AuthenticationToken)
            ?.principal
            ?.let { it as? OidcUser }
            ?.idToken
            ?.tokenValue

        if (!idToken.isNullOrBlank()) {
            builder.queryParam("id_token_hint", idToken)
        }

        return builder.build(true).toUriString()
    }
}
