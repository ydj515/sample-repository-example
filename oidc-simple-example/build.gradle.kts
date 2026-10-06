plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
}

extra["kotlin.version"] = libs.versions.kotlin.get()

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "oidc-example"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.starter.oauth2.client)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.thymeleaf)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.session.data.redis)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
