package com.example.jpajavaexample.domain.coupon.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

public record CouponUsageContext(
    BigDecimal totalPrice,
    Map<String, Long> seatCountByGrade,
    LocalDateTime usedAt
) {

    public CouponUsageContext {
        totalPrice = Objects.requireNonNullElse(totalPrice, BigDecimal.ZERO);
        seatCountByGrade = Map.copyOf(Objects.requireNonNullElse(seatCountByGrade, Map.of()));
    }

    public long seatCountFor(String gradeName) {
        return seatCountByGrade.getOrDefault(gradeName, 0L);
    }
}
