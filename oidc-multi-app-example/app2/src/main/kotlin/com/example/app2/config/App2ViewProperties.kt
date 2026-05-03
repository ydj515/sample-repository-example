package com.example.app2.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.view")
data class App2ViewProperties(
    val appName: String,
    val appDescription: String,
    val clientId: String,
    val peerAppName: String,
    val peerAppUrl: String,
    val peerAccessRoles: List<String>,
    val environmentName: String,
    val suiteName: String,
)
