package com.example.app1.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.view")
data class App1ViewProperties(
    val appName: String,
    val appDescription: String,
    val clientId: String,
    val peerAppName: String,
    val peerAppUrl: String,
    val peerAccessRoles: List<String>,
    val organizationName: String,
    val environmentName: String,
    val suiteName: String,
)
