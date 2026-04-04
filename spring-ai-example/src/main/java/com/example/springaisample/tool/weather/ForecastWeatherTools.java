package com.example.springaisample.tool.weather;

import java.io.IOException;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class ForecastWeatherTools {

    private final ForecastWeatherService forecastWeatherService;

    public ForecastWeatherTools() {
        this.forecastWeatherService = new ForecastWeatherService();
    }

    @Tool(description = """
            사용자가 요구하는 지역의 시간 별 일기 예보를 가지고 옵니다.
            지역 이름을 기반으로 latitude, longitude 정보를 조회 해서 날씨 정보를 가지고 옵니다,
            """)
    String getForecastWeather(
            @ToolParam(description = "latitude", required = true) double latitude,
            @ToolParam(description = "longitude", required = true) double longitude
    ) {
        return forecastWeatherService.getForecastWeather(latitude, longitude);
    }

    @Tool(description = "어제 날씨 정보를 가지고 옵니다. ")
    String getYesterdayWeather() throws IOException {
        return forecastWeatherService.getYesterdayWeather();
    }
}
