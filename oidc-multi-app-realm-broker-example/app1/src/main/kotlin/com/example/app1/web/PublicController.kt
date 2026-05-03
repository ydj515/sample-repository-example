package com.example.app1.web

import com.example.app1.config.App1ViewProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class PublicController(
    private val app1ViewProperties: App1ViewProperties,
) {

    @GetMapping("/public")
    fun publicEndpoint(): Map<String, Any> {
        return mapOf(
            "app" to app1ViewProperties.appName,
            "message" to "app1 공개 엔드포인트입니다. 로그인 없이 접근할 수 있습니다.",
            "next" to listOf("/oauth2/authorization/keycloak", "/api/me", app1ViewProperties.peerAppUrl),
        )
    }
}
