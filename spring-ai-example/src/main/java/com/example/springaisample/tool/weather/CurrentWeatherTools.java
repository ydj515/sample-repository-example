package com.example.springaisample.tool.weather;

import java.time.LocalDateTime;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

public class CurrentWeatherTools {

    public record WeatherResponse(Current current) {
        public record Current(LocalDateTime time, int interval, double temperature_2m) {
        }
    }

    @Tool(description = """
            오늘의 날씨 또는 현재 온도를 가지고 옵니다.
            지역 이름을 기반으로 latitude, longitude 정보를 조회 해서 날씨 정보를 가지고 옵니다,
            """)
    WeatherResponse getCurrentWeather(
            @ToolParam(description = "latitude", required = true) double latitude,
            @ToolParam(description = "longitude", required = true) double longitude
    ) {
        return RestClient.create()
                .get()
                .uri("https://api.open-meteo.com/v1/forecast?latitude={latitude}&longitude={longitude}&current=temperature_2m", latitude, longitude)
                .retrieve()
                .body(WeatherResponse.class);
    }
}
