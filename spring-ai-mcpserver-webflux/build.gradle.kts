plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
}

group = "com.ai"
version = "0.0.1-SNAPSHOT"
description = "spring-ai-mcpserver-webflux"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(libs.spring.ai.bom))
    implementation(libs.spring.ai.starter.mcp.server.webflux)
    compileOnly(libs.lombok)
    developmentOnly(libs.spring.boot.devtools)
    annotationProcessor(libs.lombok)
    testImplementation(libs.spring.boot.starter.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
