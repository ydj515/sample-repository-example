package com.example.oidccommon.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class KeycloakOidcUserService {

    private val delegate = OidcUserService()
    private val jwtDecoders = ConcurrentHashMap<String, NimbusJwtDecoder>()

    fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val user = delegate.loadUser(userRequest)
        val accessTokenClaims = decodeAccessTokenClaims(userRequest)
        val mappedAuthorities = buildSet {
            addAll(user.authorities)
            addAll(extractRealmAuthorities(user.claims))
            addAll(extractRealmAuthorities(accessTokenClaims))
            addAll(extractClientAuthorities(user.claims, userRequest.clientRegistration.clientId))
            addAll(extractClientAuthorities(accessTokenClaims, userRequest.clientRegistration.clientId))
        }
        val nameAttributeKey = if (user.claims.containsKey(PREFERRED_USERNAME)) PREFERRED_USERNAME else SUBJECT

        return if (user.userInfo != null) {
            DefaultOidcUser(mappedAuthorities, user.idToken, user.userInfo, nameAttributeKey)
        } else {
            DefaultOidcUser(mappedAuthorities, user.idToken, nameAttributeKey)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractRealmAuthorities(claims: Map<String, Any>): Set<GrantedAuthority> {
        val roles = ((claims[REALM_ACCESS] as? Map<String, Any?>)?.get(ROLES) as? Collection<*>).orEmpty()
        return roles
            .mapNotNull { it?.toString() }
            .mapTo(linkedSetOf()) { SimpleGrantedAuthority("ROLE_$it") }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractClientAuthorities(
        claims: Map<String, Any>,
        clientId: String,
    ): Set<GrantedAuthority> {
        val resourceAccess = claims[RESOURCE_ACCESS] as? Map<String, Any?> ?: return emptySet()
        val clientEntry = resourceAccess[clientId] as? Map<String, Any?> ?: return emptySet()
        val roles = clientEntry[ROLES] as? Collection<*> ?: return emptySet()
        return roles
            .mapNotNull { it?.toString() }
            .mapTo(linkedSetOf()) { SimpleGrantedAuthority("ROLE_$it") }
    }

    private fun decodeAccessTokenClaims(userRequest: OidcUserRequest): Map<String, Any> {
        val jwkSetUri = userRequest.clientRegistration.providerDetails.jwkSetUri ?: return emptyMap()
        val decoder = jwtDecoders.computeIfAbsent(jwkSetUri) {
            NimbusJwtDecoder.withJwkSetUri(it).build()
        }
        return runCatching {
            decoder.decode(userRequest.accessToken.tokenValue).claims
        }.getOrDefault(emptyMap())
    }

    private companion object {
        private const val PREFERRED_USERNAME = "preferred_username"
        private const val SUBJECT = "sub"
        private const val REALM_ACCESS = "realm_access"
        private const val RESOURCE_ACCESS = "resource_access"
        private const val ROLES = "roles"
    }
}
