package com.example.oidcsimpleexample.security

import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod

@Component
class ApiSecurityTierResolver {

    fun resolve(handler: Any): ApiSecurityLevel {
        if (handler !is HandlerMethod) {
            return ApiSecurityLevel.P2_STANDARD
        }

        handler.getMethodAnnotation(ApiSecurityTier::class.java)?.let { return it.value }
        handler.beanType.getAnnotation(ApiSecurityTier::class.java)?.let { return it.value }
        return ApiSecurityLevel.P2_STANDARD
    }
}
