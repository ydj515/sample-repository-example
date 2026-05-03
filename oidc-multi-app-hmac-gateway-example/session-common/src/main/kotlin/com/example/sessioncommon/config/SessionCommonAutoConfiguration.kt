package com.example.sessioncommon.config

import com.example.sessioncommon.security.ApiSecurityTierResolver
import com.example.sessioncommon.security.GatewaySignatureValidationFilter
import com.example.sessioncommon.security.SessionAppTaggingFilter
import com.example.sessioncommon.security.SessionRevalidationInterceptor
import com.example.sessioncommon.session.SessionLookupService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.Session

@AutoConfiguration
@EnableConfigurationProperties(SessionPolicyProperties::class)
@Import(SessionConfig::class)
class SessionCommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun apiSecurityTierResolver(): ApiSecurityTierResolver {
        return ApiSecurityTierResolver()
    }

    @Bean
    @ConditionalOnMissingBean
    fun sessionLookupService(
        sessionRepository: FindByIndexNameSessionRepository<out Session>,
    ): SessionLookupService {
        return SessionLookupService(sessionRepository)
    }

    @Bean
    @ConditionalOnMissingBean
    fun sessionRevalidationInterceptor(
        sessionPolicyProperties: SessionPolicyProperties,
        sessionLookupService: SessionLookupService,
        apiSecurityTierResolver: ApiSecurityTierResolver,
    ): SessionRevalidationInterceptor {
        return SessionRevalidationInterceptor(
            sessionPolicyProperties = sessionPolicyProperties,
            sessionLookupService = sessionLookupService,
            tierResolver = apiSecurityTierResolver,
        )
    }

    @Bean
    @ConditionalOnMissingBean
    fun sessionAppTaggingFilter(
        sessionPolicyProperties: SessionPolicyProperties,
    ): SessionAppTaggingFilter {
        return SessionAppTaggingFilter(sessionPolicyProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    fun gatewaySignatureValidationFilter(
        sessionPolicyProperties: SessionPolicyProperties,
    ): GatewaySignatureValidationFilter {
        return GatewaySignatureValidationFilter(sessionPolicyProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    fun webMvcConfig(
        sessionRevalidationInterceptor: SessionRevalidationInterceptor,
    ): WebMvcConfig {
        return WebMvcConfig(sessionRevalidationInterceptor)
    }
}
