package com.example.jpajavaexample.presentation.performance.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record PerformanceSearchRequest(
    String title,
    String category,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate startDate,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate endDate
) {
}
