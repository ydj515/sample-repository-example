package com.example.oidccommon.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest

class IdentityProviderHintAuthorizationRequestResolver(
    clientRegistrationRepository: ClientRegistrationRepository,
    private val identityProviderHint: String?,
) : OAuth2AuthorizationRequestResolver {

    private val delegate = DefaultOAuth2AuthorizationRequestResolver(
        clientRegistrationRepository,
        OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI,
    )

    override fun resolve(request: HttpServletRequest): OAuth2AuthorizationRequest? {
        return delegate.resolve(request)?.withIdentityProviderHint()
    }

    override fun resolve(
        request: HttpServletRequest,
        clientRegistrationId: String,
    ): OAuth2AuthorizationRequest? {
        return delegate.resolve(request, clientRegistrationId)?.withIdentityProviderHint()
    }

    private fun OAuth2AuthorizationRequest.withIdentityProviderHint(): OAuth2AuthorizationRequest {
        val hint = identityProviderHint?.takeIf(String::isNotBlank) ?: return this
        return OAuth2AuthorizationRequest
            .from(this)
            .additionalParameters(additionalParameters + (KC_IDP_HINT to hint))
            .build()
    }

    companion object {
        private const val KC_IDP_HINT = "kc_idp_hint"
    }
}
