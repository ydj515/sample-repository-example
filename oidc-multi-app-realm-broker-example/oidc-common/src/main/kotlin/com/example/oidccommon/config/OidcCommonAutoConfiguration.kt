package com.example.oidccommon.config

import com.example.oidccommon.security.KeycloakLogoutSuccessHandler
import com.example.oidccommon.security.OidcSecurityConfigurer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper
import org.springframework.security.web.SecurityFilterChain
import java.net.URI

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(OidcSecurityProperties::class)
class OidcCommonAutoConfiguration {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        oidcSecurityConfigurer: OidcSecurityConfigurer,
    ): SecurityFilterChain {
        return oidcSecurityConfigurer.build(http)
    }

    @Bean
    fun keycloakLogoutSuccessHandler(
        oidcSecurityProperties: OidcSecurityProperties,
    ): KeycloakLogoutSuccessHandler {
        return KeycloakLogoutSuccessHandler(
            endSessionUri = URI.create(oidcSecurityProperties.endSessionUri),
            brokerLogoutEnabled = oidcSecurityProperties.brokerLogoutEnabled(),
        )
    }

    @Bean
    fun grantedAuthoritiesMapper(): GrantedAuthoritiesMapper = GrantedAuthoritiesMapper { authorities -> authorities }
}
