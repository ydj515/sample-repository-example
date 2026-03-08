package com.example.oidcsimpleexample.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.security")
data class AppSecurityProperties(
    val endSessionUri: String,
    val revalidation: RevalidationProperties = RevalidationProperties(),
)

data class RevalidationProperties(
    val standardTtl: Duration = Duration.ofSeconds(5),
    val sensitiveTtl: Duration = Duration.ofSeconds(1),
)
