package com.example.oidcsimpleexample

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class OidcSimpleExampleApplication

fun main(args: Array<String>) {
    runApplication<com.example.oidcsimpleexample.OidcSimpleExampleApplication>(*args)
}
