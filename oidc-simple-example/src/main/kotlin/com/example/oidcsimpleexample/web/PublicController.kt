package com.example.oidcsimpleexample.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class PublicController {

    @GetMapping("/public")
    fun publicEndpoint(): Map<String, Any> {
        return mapOf(
            "message" to "로그인 없이 접근 가능한 공개 엔드포인트입니다.",
            "next" to listOf("/oauth2/authorization/keycloak", "/api/me"),
        )
    }
}
