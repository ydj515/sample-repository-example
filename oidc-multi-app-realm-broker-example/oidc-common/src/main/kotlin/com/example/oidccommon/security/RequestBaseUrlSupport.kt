package com.example.oidccommon.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.util.UriComponentsBuilder

internal fun HttpServletRequest.baseUrl(): String {
    val builder = UriComponentsBuilder
        .newInstance()
        .scheme(scheme)
        .host(serverName)

    if (serverPort != 80 && serverPort != 443) {
        builder.port(serverPort)
    }

    return builder
        .path("/")
        .build()
        .toUriString()
}
