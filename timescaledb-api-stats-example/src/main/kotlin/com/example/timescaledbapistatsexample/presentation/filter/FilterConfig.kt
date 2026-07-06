package com.example.timescaledbapistatsexample.presentation.filter

import com.example.timescaledbapistatsexample.application.ApiAccessService
import com.example.timescaledbapistatsexample.application.ApiCallEventPublisher
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
class FilterConfig {
    @Bean
    fun apiCallCaptureFilter(publisher: ApiCallEventPublisher): FilterRegistrationBean<ApiCallCaptureFilter> {
        return FilterRegistrationBean(ApiCallCaptureFilter(publisher)).apply {
            order = Ordered.HIGHEST_PRECEDENCE + 10
            addUrlPatterns("/*")
        }
    }

    @Bean
    fun apiKeyAuthFilter(apiAccessService: ApiAccessService): FilterRegistrationBean<ApiKeyAuthFilter> {
        return FilterRegistrationBean(ApiKeyAuthFilter(apiAccessService)).apply {
            order = Ordered.HIGHEST_PRECEDENCE + 20
            addUrlPatterns("/*")
        }
    }
}
