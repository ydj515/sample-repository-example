plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.kotlin.jpa)
	alias(libs.plugins.dependency.management)
}

extra["kotlin.version"] = libs.versions.kotlin.get()

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.spring.boot.starter.web)
	implementation(libs.jackson.module.kotlin)
	implementation(libs.kotlinx.coroutines.core)
	implementation(libs.kotlinx.coroutines.slf4j)
	implementation(libs.kotlin.reflect)
	runtimeOnly(libs.h2)
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.spring.boot.testcontainers)
	testImplementation(libs.kotlin.test.junit5)
	testImplementation(libs.testcontainers.junit.jupiter)
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
