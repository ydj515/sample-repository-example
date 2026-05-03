package com.example.sessioncommon.security

import org.springframework.web.method.HandlerMethod

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
