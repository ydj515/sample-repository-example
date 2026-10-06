plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
}

extra["kotlin.version"] = libs.versions.kotlin.get()

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // 부트 웹 의존성에서 기본 로깅(starter-logging)만 제외
    implementation(libs.spring.boot.starter.web) {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }

    // 필요하면 JDK 로깅만 쓰거나, 나중에 다른 로깅 구현(slf4j-simple 등)을 추가할 수 있음
    implementation(libs.kotlin.reflect)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}