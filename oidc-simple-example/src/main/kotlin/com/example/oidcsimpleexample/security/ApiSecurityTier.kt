package com.example.oidcsimpleexample.security

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiSecurityTier(
    val value: ApiSecurityLevel,
)
