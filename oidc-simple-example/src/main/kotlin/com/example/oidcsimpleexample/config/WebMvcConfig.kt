package com.example.oidcsimpleexample.config

import com.example.oidcsimpleexample.security.SessionRevalidationInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val sessionRevalidationInterceptor: SessionRevalidationInterceptor,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry
            .addInterceptor(sessionRevalidationInterceptor)
            .addPathPatterns("/api/**")
    }
}
