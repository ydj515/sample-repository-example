package com.example.jpajavaexample.domain.performance.service;

import java.math.BigDecimal;

public record PerformanceSeatGradeInfo(
    String gradeName,
    long totalSeats,
    long availableSeats,
    BigDecimal minPrice,
    BigDecimal maxPrice
) {
}
