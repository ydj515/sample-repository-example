package com.example.jpajavaexample.presentation.performance.dto;

import com.example.jpajavaexample.domain.performance.service.PerformanceDetailInfo;
import com.example.jpajavaexample.domain.performance.service.PerformanceScheduleInfo;
import com.example.jpajavaexample.domain.performance.service.PerformanceSeatGradeInfo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PerformanceDetailResponse(
    Long performanceId,
    String title,
    String category,
    String posterUrl,
    LocalDate startDate,
    LocalDate endDate,
    List<PerformanceScheduleDetailResponse> schedules
) {

    public static PerformanceDetailResponse from(PerformanceDetailInfo info) {
        List<PerformanceScheduleDetailResponse> schedules = info.schedules().stream()
            .map(PerformanceScheduleDetailResponse::from)
            .toList();

        return new PerformanceDetailResponse(
            info.performanceId(),
            info.title(),
            info.category(),
            info.posterUrl(),
            info.startDate(),
            info.endDate(),
            List.copyOf(schedules)
        );
    }

    public record PerformanceScheduleDetailResponse(
        Long scheduleId,
        LocalDateTime startsAt,
        List<PerformanceSeatGradeDetailResponse> seatGrades
    ) {
        private static PerformanceScheduleDetailResponse from(PerformanceScheduleInfo info) {
            List<PerformanceSeatGradeDetailResponse> seatGrades = info.seatGrades().stream()
                .map(PerformanceSeatGradeDetailResponse::from)
                .toList();

            return new PerformanceScheduleDetailResponse(
                info.scheduleId(),
                info.startsAt(),
                List.copyOf(seatGrades)
            );
        }
    }

    public record PerformanceSeatGradeDetailResponse(
        String gradeName,
        long totalSeats,
        long availableSeats,
        BigDecimal minPrice,
        BigDecimal maxPrice
    ) {
        private static PerformanceSeatGradeDetailResponse from(PerformanceSeatGradeInfo info) {
            return new PerformanceSeatGradeDetailResponse(
                info.gradeName(),
                info.totalSeats(),
                info.availableSeats(),
                info.minPrice(),
                info.maxPrice()
            );
        }
    }
}
