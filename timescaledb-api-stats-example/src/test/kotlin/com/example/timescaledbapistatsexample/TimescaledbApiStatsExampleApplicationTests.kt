package com.example.timescaledbapistatsexample

import kotlin.test.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:api_stats_test",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "api-stats.auth.load-on-startup=false",
        "api-stats.consumer.enabled=false",
    ],
)
class TimescaledbApiStatsExampleApplicationTests {
    @Test
    fun contextLoads() {
    }
}
