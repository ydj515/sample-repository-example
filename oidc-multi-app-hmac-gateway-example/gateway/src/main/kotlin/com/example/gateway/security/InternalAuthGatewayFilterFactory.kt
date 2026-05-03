package com.example.gateway.security

import com.example.internalauth.InternalAuthHeaders
import com.example.internalauth.InternalAuthPayload
import com.example.internalauth.InternalAuthSigner
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpCookie
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class InternalAuthGatewayFilterFactory(
    private val signer: InternalAuthSigner,
) : AbstractGatewayFilterFactory<InternalAuthGatewayFilterFactory.Config>(Config::class.java) {

    var clock: Clock = Clock.systemUTC()

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val request = exchange.request
            val sessionId = request.cookies[config.sessionCookieName]
                ?.firstOrNull()
                ?.let(HttpCookie::getValue)
                .orEmpty()
            val issuedAt = Instant.now(clock).epochSecond
            val path = request.uri.rawPath
            val method = request.method.name()
            val payload = InternalAuthPayload(
                appId = config.appId,
                method = method,
                path = path,
                sessionId = sessionId,
                issuedAtEpochSeconds = issuedAt,
            )
            val signature = signer.sign(payload)

            val mutatedRequest = request.mutate()
                .headers { headers ->
                    InternalAuthHeaders.all.forEach(headers::remove)
                    headers.set(InternalAuthHeaders.APP_ID, payload.appId)
                    headers.set(InternalAuthHeaders.METHOD, payload.method)
                    headers.set(InternalAuthHeaders.PATH, payload.path)
                    headers.set(InternalAuthHeaders.SESSION_ID, payload.sessionId)
                    headers.set(InternalAuthHeaders.ISSUED_AT, payload.issuedAtEpochSeconds.toString())
                    headers.set(InternalAuthHeaders.SIGNATURE, signature)
                }
                .build()

            chain.filter(exchange.mutate().request(mutatedRequest).build())
        }
    }

    data class Config(
        var appId: String = "",
        var sessionCookieName: String = "",
    )
}
