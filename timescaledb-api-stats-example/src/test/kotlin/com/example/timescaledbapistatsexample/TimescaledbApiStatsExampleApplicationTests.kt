package com.example.timescaledbapistatsexample

import kotlin.test.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:api_stats_test",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "api-stats.auth.load-on-startup=false",
        "api-stats.consumer.enabled=false",
        // H2에는 스키마가 없다. 주기 갱신이 계속 실패 로그를 찍지 않도록 간격을 늘린다.
        "api-stats.auth.refresh-interval-ms=3600000",
    ],
)
class TimescaledbApiStatsExampleApplicationTests {
    @Test
    fun contextLoads() {
    }
}
