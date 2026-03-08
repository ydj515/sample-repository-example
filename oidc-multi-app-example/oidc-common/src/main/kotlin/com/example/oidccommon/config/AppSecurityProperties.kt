package com.example.oidccommon.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.security.core.GrantedAuthority
import java.time.Duration

@ConfigurationProperties(prefix = "app.security")
data class AppSecurityProperties(
    val appId: String,
    val endSessionUri: String,
    val logoutCookieName: String = "SESSION",
    val access: AccessProperties = AccessProperties(),
    val revalidation: RevalidationProperties = RevalidationProperties(),
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

    private fun List<String>.mapToAuthorities(): Array<String> {
        return distinct()
            .map { "ROLE_$it" }
            .toTypedArray()
    }
}

data class AccessProperties(
    val userRoles: List<String> = emptyList(),
    val adminRoles: List<String> = emptyList(),
    val masterAdminRole: String = "master-admin",
)

data class RevalidationProperties(
    val standardTtl: Duration = Duration.ofSeconds(5),
    val sensitiveTtl: Duration = Duration.ofSeconds(1),
)
