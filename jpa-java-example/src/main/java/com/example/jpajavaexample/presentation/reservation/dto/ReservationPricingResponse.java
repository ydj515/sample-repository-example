package com.example.jpajavaexample.presentation.reservation.dto;

import com.example.jpajavaexample.domain.Reservation;
import java.math.BigDecimal;

public record ReservationPricingResponse(
    Long reservationId,
    Long couponId,
    BigDecimal totalPrice,
    BigDecimal discountAmount,
    BigDecimal finalPrice
) {

    public static ReservationPricingResponse from(Reservation reservation) {
        BigDecimal total = reservation.calculateTotalPrice();
        BigDecimal discount = reservation.getDiscountAmount();
        BigDecimal finalPrice = reservation.calculateFinalPrice();
        Long couponId = reservation.getCoupon() == null ? null : reservation.getCoupon().getId();

        return new ReservationPricingResponse(
            reservation.getId(),
            couponId,
            total,
            discount,
            finalPrice
        );
    }
}
