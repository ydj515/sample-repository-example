plugins {
    java
    id("org.springframework.boot") version "3.5.12"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "spring-ai-example"

val springAiVersion = "1.1.4"

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
    implementation(platform("org.springframework.ai:spring-ai-bom:$springAiVersion"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    // OpenAI 연동용 Spring AI 스타터
    implementation("org.springframework.ai:spring-ai-starter-model-openai")

    // Claude(Anthropic) 연동용 Spring AI 스타터
    implementation("org.springframework.ai:spring-ai-starter-model-anthropic")
    implementation("org.springframework.ai:spring-ai-template-st")
    // client-webflux는 mcpserver로 webflux, mvc, stdio 전부 대응 가능해서 webflux로 사용
    implementation("org.springframework.ai:spring-ai-starter-mcp-client-webflux")

    // PgVector VectorStore
    implementation("org.springframework.ai:spring-ai-starter-vector-store-pgvector")
    implementation("org.springframework.ai:spring-ai-advisors-vector-store")
    runtimeOnly("org.postgresql:postgresql")

    // RAG 및 문서 리더
    implementation("org.springframework.ai:spring-ai-rag")
    implementation("org.springframework.ai:spring-ai-pdf-document-reader")
    implementation("org.springframework.ai:spring-ai-tika-document-reader")

    // JDBC Chat Memory
    implementation("org.springframework.ai:spring-ai-starter-model-chat-memory-repository-jdbc")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
