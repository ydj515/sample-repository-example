package com.example.jpajavaexample.presentation.performance.dto;

import com.example.jpajavaexample.domain.performance.model.Performance;
import java.time.LocalDate;

public record PerformanceSummaryResponse(
    Long id,
    String title,
    String category,
    LocalDate startDate,
    LocalDate endDate
) {
    public static PerformanceSummaryResponse from(Performance performance) {
        return new PerformanceSummaryResponse(
            performance.getId(),
            performance.getTitle(),
            performance.getCategory(),
            performance.getStartDate(),
            performance.getEndDate()
        );
    }
}
