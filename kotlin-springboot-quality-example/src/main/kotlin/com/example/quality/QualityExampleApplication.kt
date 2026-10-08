package com.example.quality

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class QualityExampleApplication

// Spring Boot accepts varargs; copying startup arguments once is intentional.
@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<QualityExampleApplication>(*args)
}
