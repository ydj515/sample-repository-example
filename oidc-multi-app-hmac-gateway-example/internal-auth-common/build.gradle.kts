plugins {
    alias(libs.plugins.kotlin.spring)
    `java-library`
}

dependencies {
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}
