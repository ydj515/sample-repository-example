import com.github.spotbugs.snom.SpotBugsTask
import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.Pmd
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    java
    checkstyle
    pmd
    jacoco
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.spotless)
}

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

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

dependencies {
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.web)
    runtimeOnly(libs.h2)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.archunit)
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}
tasks.withType<Checkstyle>().configureEach {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

pmd {
    toolVersion = libs.versions.pmd.get()
    ruleSets = emptyList()
    ruleSetFiles = files(rootProject.file("config/pmd/ruleset.xml"))
    isConsoleOutput = true
    isIgnoreFailures = false
    threads = 1
}
tasks.withType<Pmd>().configureEach {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

spotbugs {
    toolVersion.set(libs.versions.spotbugs.get())
    ignoreFailures = false
    showProgress = false
    effort.set(Effort.MAX)
    reportLevel.set(Confidence.MEDIUM)
    excludeFilter = rootProject.file("config/spotbugs/exclude-filter.xml")
}
tasks.withType<SpotBugsTask>().configureEach {
    reports.create("html") { required.set(true) }
    reports.create("xml") { required.set(true) }
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat(libs.versions.googleJavaFormat.get())
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

// All authored production classes remain in both the report and the gate.
tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("jacocoTestReport"))
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                minimum = "0.70".toBigDecimal()
            }
        }
        rule {
            element = "CLASS"
            includes = listOf("com.example.quality.application.*")
            limit {
                counter = "LINE"
                minimum = "0.90".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

val architectureTest by tasks.registering(Test::class) {
    group = "verification"
    description = "Checks the dependency direction between application layers."
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter { includeTestsMatching("*ArchitectureTest*") }
    useJUnitPlatform()
}
tasks.named("check") {
    dependsOn(
        "spotlessCheck",
        "spotbugsMain",
        "spotbugsTest",
        "jacocoTestCoverageVerification",
        architectureTest,
    )
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Unit and H2 integration tests share test.exec; architecture tests run separately.
tasks.named<Test>("test") {
    exclude("**/*ArchitectureTest*")
    finalizedBy("jacocoTestReport")
}
