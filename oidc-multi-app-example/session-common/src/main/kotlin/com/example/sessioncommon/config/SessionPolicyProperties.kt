package com.example.sessioncommon.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.session")
data class SessionPolicyProperties(
    val appId: String,
    val revalidation: RevalidationProperties = RevalidationProperties(),
)

data class RevalidationProperties(
    val standardTtl: Duration = Duration.ofSeconds(5),
    val sensitiveTtl: Duration = Duration.ofSeconds(1),
)
