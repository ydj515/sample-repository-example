package com.example.app2

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.example.app2", "com.example.oidccommon"])
@ConfigurationPropertiesScan(basePackages = ["com.example.app2", "com.example.oidccommon"])
class App2Application

fun main(args: Array<String>) {
    runApplication<App2Application>(*args)
}
