package com.ai.app.service;

import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

@Service
@Slf4j
public class WeatherMcpTool {


    public record WeatherResponse(Current current) {
        public record Current(LocalDateTime time, int interval, double temperature_2m) {}
    }

    @McpTool(description = """
            오늘의 날씨 또는 현재 온도를 가지고 옵니다.
            지역 이름을 기반으로 latitude, longitude 정보를 조회 해서 날씨 정보를 가지고 옵니다.
            """)
    public WeatherResponse getTemperature(@McpToolParam(description = "latitude") double latitude,
                                          @McpToolParam(description = "longitude") double longitude) {
        log.info("Weather Service {}, {}", latitude, longitude);
        return RestClient.create()
                .get()
                .uri("https://api.open-meteo.com/v1/forecast?latitude={latitude}&longitude={longitude}&current=temperature_2m",  latitude, longitude)
                .retrieve()
                .body(WeatherResponse.class);
    }

}
