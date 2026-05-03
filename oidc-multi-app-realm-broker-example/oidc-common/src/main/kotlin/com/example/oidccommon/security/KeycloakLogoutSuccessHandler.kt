package com.example.oidccommon.security

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
    private val brokerLogoutEnabled: Boolean,
) : LogoutSuccessHandler {

    override fun onLogoutSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?,
    ) {
        response.sendRedirect(buildTargetUrl(request, authentication))
    }

    private fun buildTargetUrl(
        request: HttpServletRequest,
        authentication: Authentication?,
    ): String {
        val baseUrl = request.baseUrl()
        val postLogoutRedirectUri = if (brokerLogoutEnabled) {
            UriComponentsBuilder
                .fromUriString(baseUrl)
                .path(BrokerLogoutController.BROKER_LOGOUT_PATH.removePrefix("/"))
                .build()
                .toUriString()
        } else {
            baseUrl
        }

        val builder = UriComponentsBuilder
            .fromUri(endSessionUri)
            .queryParam("post_logout_redirect_uri", postLogoutRedirectUri)

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
