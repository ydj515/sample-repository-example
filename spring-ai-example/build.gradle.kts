plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "spring-ai-example"

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
    implementation(libs.spring.boot.starter.web)

    // OpenAI 연동용 Spring AI 스타터
    implementation(libs.spring.ai.starter.model.openai)

    // Claude(Anthropic) 연동용 Spring AI 스타터
    implementation(libs.spring.ai.starter.model.anthropic)
    implementation(libs.spring.ai.template.st)
    // client-webflux는 mcpserver로 webflux, mvc, stdio 전부 대응 가능해서 webflux로 사용
    implementation(libs.spring.ai.starter.mcp.client.webflux)

    // PgVector VectorStore
    implementation(libs.spring.ai.starter.vector.store.pgvector)
    implementation(libs.spring.ai.advisors.vector.store)
    runtimeOnly(libs.postgresql)

    // RAG 및 문서 리더
    implementation(libs.spring.ai.rag)
    implementation(libs.spring.ai.pdf.document.reader)
    implementation(libs.spring.ai.tika.document.reader)

    // JDBC Chat Memory
    implementation(libs.spring.ai.starter.model.chat.memory.repository.jdbc)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testImplementation(libs.spring.boot.starter.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
