package com.example.app2.web

import com.example.app2.config.App2ViewProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class PublicController(
    private val app2ViewProperties: App2ViewProperties,
) {

    @GetMapping("/public")
    fun publicEndpoint(): Map<String, Any> {
        return mapOf(
            "app" to app2ViewProperties.appName,
            "message" to "App2 공개 엔드포인트입니다. App1과는 다른 OIDC client로 동작합니다.",
            "next" to listOf("/oauth2/authorization/keycloak", "/api/me", app2ViewProperties.peerAppUrl),
        )
    }
}
