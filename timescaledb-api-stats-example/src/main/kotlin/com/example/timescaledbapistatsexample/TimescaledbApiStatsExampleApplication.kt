package com.example.timescaledbapistatsexample

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class TimescaledbApiStatsExampleApplication

fun main(args: Array<String>) {
    runApplication<TimescaledbApiStatsExampleApplication>(*args)
}
