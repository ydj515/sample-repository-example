package com.example.sessioncommon.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.session")
data class SessionPolicyProperties(
    val appId: String,
    val revalidation: RevalidationProperties = RevalidationProperties(),
    val internalAuth: InternalAuthProperties = InternalAuthProperties(),
)

data class RevalidationProperties(
    val standardTtl: Duration = Duration.ofSeconds(5),
    val sensitiveTtl: Duration = Duration.ofSeconds(1),
)

data class InternalAuthProperties(
    val enabled: Boolean = false,
    val required: Boolean = false,
    val secret: String = "local-dev-internal-auth-secret-change-me",
    val maxAge: Duration = Duration.ofSeconds(30),
    val protectedPathPatterns: List<String> = listOf("/api/**"),
)
