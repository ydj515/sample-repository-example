package com.example.oidcsimpleexample.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service

@Service
class KeycloakOidcUserService {

    private val delegate = OidcUserService()

    fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val user = delegate.loadUser(userRequest)
        val mappedAuthorities = buildSet {
            addAll(user.authorities)
            addAll(extractRealmAuthorities(user))
            addAll(extractClientAuthorities(user, userRequest))
        }
        val nameAttributeKey = if (user.claims.containsKey(PREFERRED_USERNAME)) PREFERRED_USERNAME else SUBJECT

        return if (user.userInfo != null) {
            DefaultOidcUser(mappedAuthorities, user.idToken, user.userInfo, nameAttributeKey)
        } else {
            DefaultOidcUser(mappedAuthorities, user.idToken, nameAttributeKey)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractRealmAuthorities(user: OidcUser): Set<GrantedAuthority> {
        val roles = ((user.claims[REALM_ACCESS] as? Map<String, Any?>)?.get(ROLES) as? Collection<*>).orEmpty()
        return roles
            .mapNotNull { it?.toString() }
            .mapTo(linkedSetOf()) { SimpleGrantedAuthority("ROLE_$it") }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractClientAuthorities(
        user: OidcUser,
        userRequest: OidcUserRequest,
    ): Set<GrantedAuthority> {
        val resourceAccess = user.claims[RESOURCE_ACCESS] as? Map<String, Any?> ?: return emptySet()
        val clientEntry = resourceAccess[userRequest.clientRegistration.clientId] as? Map<String, Any?> ?: return emptySet()
        val roles = clientEntry[ROLES] as? Collection<*> ?: return emptySet()
        return roles
            .mapNotNull { it?.toString() }
            .mapTo(linkedSetOf()) { SimpleGrantedAuthority("ROLE_$it") }
    }

    private companion object {
        private const val PREFERRED_USERNAME = "preferred_username"
        private const val SUBJECT = "sub"
        private const val REALM_ACCESS = "realm_access"
        private const val RESOURCE_ACCESS = "resource_access"
        private const val ROLES = "roles"
    }
}
