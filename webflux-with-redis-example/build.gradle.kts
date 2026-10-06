plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
}

group = "com.example"
version = "0.0.1-SNAPSHOT"


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
    /* spring */
    implementation(libs.spring.boot.starter.webflux)
    developmentOnly(libs.spring.boot.devtools)

    /* swagger */
    implementation(libs.springdoc.openapi)
    /* redis */
    implementation(libs.spring.boot.starter.data.redis.reactive)
    implementation(libs.redisson)

    /* gson */
    implementation(libs.gson)

    /* mapstruct */
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    annotationProcessor(libs.lombok.mapstruct.binding)

    /* test */
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.reactor.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
    inputs.property("runRedisUsecaseTests", providers.environmentVariable("RUN_REDIS_USECASE_TESTS").orElse("true"))
}
