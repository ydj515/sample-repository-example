plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "timescaledb-api-stats-example"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    // 실제 TimescaleDB에 붙어야만 드러나는 동작(continuous aggregate, 압축, 롤업 정확도)을 검증한다.
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()

    // Testcontainers가 쓰는 docker-java의 기본 클라이언트 API 버전은 1.32라,
    // 최신 Docker 엔진(OrbStack, Docker 25+)이 "client version 1.32 is too old"로 거절한다.
    // 통합 테스트가 조용히 skip 되지 않도록 버전을 올려서 넘긴다.
    systemProperty("api.version", System.getenv("DOCKER_API_VERSION") ?: "1.44")

    // 셸에서 지정한 Docker 접속 정보를 테스트 JVM까지 전달한다.
    System.getenv("DOCKER_HOST")?.let { environment("DOCKER_HOST", it) }

    testLogging {
        events("skipped", "failed")
    }
}
