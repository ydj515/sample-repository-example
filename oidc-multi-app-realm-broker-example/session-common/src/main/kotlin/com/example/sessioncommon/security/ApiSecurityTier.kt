package com.example.sessioncommon.security

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiSecurityTier(
    val value: ApiSecurityLevel,
)
