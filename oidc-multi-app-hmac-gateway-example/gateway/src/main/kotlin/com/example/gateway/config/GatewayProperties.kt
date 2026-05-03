package com.example.gateway.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI

@ConfigurationProperties(prefix = "app.gateway")
data class GatewayProperties(
    val app1Uri: URI = URI.create("http://localhost:8081"),
    val app2Uri: URI = URI.create("http://localhost:8082"),
)

@ConfigurationProperties(prefix = "app.internal-auth")
data class GatewayInternalAuthProperties(
    val secret: String = "local-dev-internal-auth-secret-change-me",
)
