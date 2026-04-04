package com.ai.app.service;

import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class WeatherForecastMcpTool {

    @McpTool(description = """
             사용자가 요구하는 지역의 시간 별 일기 예보를 가지고 옵니다.
            지역 이름을 기반으로 latitude, longitude 정보를 조회 해서 날씨 정보를 가지고 옵니다,
            """)
    String getWeatherForecast(@McpToolParam(description = "latitude") double latitude, @McpToolParam(description = "longitude") double longitude) {
        log.info("Weather WeatherForecastMcpTool {}, {}", latitude, longitude);

        return RestClient.create()
                .get()
                .uri("https://api.open-meteo.com/v1/forecast?latitude={latitude}&longitude={longitude}&hourly=temperature_2m",  latitude, longitude)
                .retrieve().body(String.class);
    }

}
