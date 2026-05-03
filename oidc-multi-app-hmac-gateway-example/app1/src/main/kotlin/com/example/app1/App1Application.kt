package com.example.app1

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.example.app1", "com.example.oidccommon"])
@ConfigurationPropertiesScan(basePackages = ["com.example.app1", "com.example.oidccommon"])
class App1Application

fun main(args: Array<String>) {
    runApplication<App1Application>(*args)
}
