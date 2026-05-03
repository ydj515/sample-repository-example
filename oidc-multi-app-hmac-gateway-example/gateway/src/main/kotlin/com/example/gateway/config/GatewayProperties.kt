package com.example.gateway.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI

@ConfigurationProperties(prefix = "app.gateway")
class GatewayProperties {
    var app1Uri: URI = URI.create("http://localhost:8081")
    var app2Uri: URI = URI.create("http://localhost:8082")
}

@ConfigurationProperties(prefix = "app.internal-auth")
class GatewayInternalAuthProperties {
    var secret: String = "local-dev-internal-auth-secret-change-me"
}
