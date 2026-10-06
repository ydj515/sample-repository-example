plugins {
    alias(libs.plugins.kotlin.spring)
    `java-library`
}

dependencies {
    api(project(":session-common"))
    api(libs.spring.boot.starter.oauth2.client)
    api(libs.spring.boot.starter.security)
    api(libs.jackson.module.kotlin)
    api(libs.kotlin.reflect)

    testImplementation(libs.spring.security.test)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}
