plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.cloud.dependencies.get().toString())
    }
}

dependencies {
    implementation(project(":internal-auth-common"))
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.cloud.starter.gateway.server.webflux)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.reactor.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
