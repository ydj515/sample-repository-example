package com.example.jpajavaexample.domain.performance.service;

import java.time.LocalDate;
import java.util.List;

public record PerformanceDetailInfo(
    Long performanceId,
    String title,
    String category,
    String posterUrl,
    LocalDate startDate,
    LocalDate endDate,
    List<PerformanceScheduleInfo> schedules
) {
}
