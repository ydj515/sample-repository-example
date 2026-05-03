package com.example.sessioncommon.config

import com.example.sessioncommon.security.SessionRevalidationInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

class WebMvcConfig(
    private val sessionRevalidationInterceptor: SessionRevalidationInterceptor,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry
            .addInterceptor(sessionRevalidationInterceptor)
            .addPathPatterns("/api/**")
    }
}
