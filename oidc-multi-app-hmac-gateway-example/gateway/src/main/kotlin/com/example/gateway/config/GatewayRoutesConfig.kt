package com.example.gateway.config

import com.example.gateway.security.InternalAuthGatewayFilterFactory
import com.example.gateway.security.InternalAuthGatewayFilterFactory.Config
import com.example.internalauth.InternalAuthSigner
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GatewayRoutesConfig {

    @Bean
    fun internalAuthSigner(
        gatewayInternalAuthProperties: GatewayInternalAuthProperties,
    ): InternalAuthSigner {
        return InternalAuthSigner(gatewayInternalAuthProperties.secret)
    }

    @Bean
    fun gatewayRoutes(
        builder: RouteLocatorBuilder,
        gatewayProperties: GatewayProperties,
        internalAuthGatewayFilterFactory: InternalAuthGatewayFilterFactory,
    ): RouteLocator {
        return builder.routes()
            .route("app1") { route ->
                route
                    .path("/app1/**")
                    .filters { filters ->
                        filters
                            .stripPrefix(1)
                            .filter(
                                internalAuthGatewayFilterFactory.apply(
                                    Config(appId = "app1", sessionCookieName = "APP1SESSION"),
                                ),
                            )
                    }
                    .uri(gatewayProperties.app1Uri.toString())
            }
            .route("app2") { route ->
                route
                    .path("/app2/**")
                    .filters { filters ->
                        filters
                            .stripPrefix(1)
                            .filter(
                                internalAuthGatewayFilterFactory.apply(
                                    Config(appId = "app2", sessionCookieName = "APP2SESSION"),
                                ),
                            )
                    }
                    .uri(gatewayProperties.app2Uri.toString())
            }
            .build()
    }
}
