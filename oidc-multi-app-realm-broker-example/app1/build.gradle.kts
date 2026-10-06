plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":oidc-common"))
    implementation(project(":session-common"))
    implementation(libs.spring.boot.starter.thymeleaf)

    testImplementation(libs.spring.security.test)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}
