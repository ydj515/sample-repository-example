package com.example.oidccommon.security

import com.example.oidccommon.config.OidcSecurityProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.util.UriComponentsBuilder

@Controller
class BrokerLogoutController(
    private val oidcSecurityProperties: OidcSecurityProperties,
) {

    @GetMapping(BROKER_LOGOUT_PATH)
    fun brokerLogout(request: HttpServletRequest): org.springframework.http.ResponseEntity<Void> {
        val baseUrl = request.baseUrl()
        val brokerLogout = oidcSecurityProperties.brokerLogout

        val targetUrl = if (brokerLogout == null) {
            baseUrl
        } else {
            UriComponentsBuilder
                .fromUriString(brokerLogout.endSessionUri)
                .queryParam("client_id", brokerLogout.clientId)
                .queryParam("post_logout_redirect_uri", baseUrl)
                .build(true)
                .toUriString()
        }

        return org.springframework.http.ResponseEntity
            .status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, targetUrl)
            .build()
    }

    companion object {
        const val BROKER_LOGOUT_PATH: String = "/logout/broker"
    }
}
