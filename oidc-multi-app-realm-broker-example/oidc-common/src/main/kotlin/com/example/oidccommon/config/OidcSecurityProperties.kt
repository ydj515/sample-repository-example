package com.example.oidccommon.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.security.core.GrantedAuthority

@ConfigurationProperties(prefix = "app.security")
data class OidcSecurityProperties(
    val endSessionUri: String,
    val logoutCookieName: String = "SESSION",
    val identityProviderHint: String? = null,
    val brokerLogout: BrokerLogoutProperties? = null,
    val access: AccessProperties = AccessProperties(),
) {
    fun accessAuthorities(): Array<String> {
        return (access.userRoles + access.adminRoles + access.masterAdminRole)
            .mapToAuthorities()
    }

    fun adminAuthorities(): Array<String> {
        return (access.adminRoles + access.masterAdminRole)
            .mapToAuthorities()
    }

    fun hasAccess(authorities: Collection<GrantedAuthority>): Boolean {
        val granted = authorities.mapTo(linkedSetOf()) { it.authority }
        return accessAuthorities().any(granted::contains)
    }

    fun isMasterAdmin(authorities: Collection<GrantedAuthority>): Boolean {
        return authorities.any { it.authority == "ROLE_${access.masterAdminRole}" }
    }

    fun brokerLogoutEnabled(): Boolean {
        val broker = brokerLogout ?: return false
        return broker.endSessionUri.isNotBlank() && broker.clientId.isNotBlank()
    }

    private fun List<String>.mapToAuthorities(): Array<String> {
        return distinct()
            .map { "ROLE_$it" }
            .toTypedArray()
    }
}

data class BrokerLogoutProperties(
    val endSessionUri: String,
    val clientId: String,
)

data class AccessProperties(
    val userRoles: List<String> = emptyList(),
    val adminRoles: List<String> = emptyList(),
    val masterAdminRole: String = "master-admin",
)
