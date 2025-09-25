package com.example.jpajavaexample.domain.performance.service;

import java.time.LocalDateTime;
import java.util.List;

public record PerformanceScheduleInfo(
    Long scheduleId,
    LocalDateTime startsAt,
    List<PerformanceSeatGradeInfo> seatGrades
) {
}
