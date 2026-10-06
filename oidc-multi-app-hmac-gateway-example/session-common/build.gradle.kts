plugins {
    alias(libs.plugins.kotlin.spring)
    `java-library`
}

dependencies {
    api(project(":internal-auth-common"))
    api(libs.spring.boot.starter.security)
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.data.redis)
    api(libs.spring.session.data.redis)
    api(libs.jackson.module.kotlin)
    api(libs.kotlin.reflect)

    testImplementation(libs.spring.security.test)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}
