package com.example.springaisample.tool.weather;

import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ForecastWeatherService {

    public String getForecastWeather(double latitude, double longitude) {
        return RestClient.create()
                .get()
                .uri("https://api.open-meteo.com/v1/forecast?latitude={latitude}&longitude={longitude}&hourly=temperature_2m", latitude, longitude)
                .retrieve()
                .body(String.class);
    }

    public String getYesterdayWeather() throws IOException {
        // 데이터 조회 시 예외 상황 발생
        // ToolExecutionConfig에서 처리
        throw new IOException("Connection Error");
    }
}
