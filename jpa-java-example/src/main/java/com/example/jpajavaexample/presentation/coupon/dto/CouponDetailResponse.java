package com.example.jpajavaexample.presentation.coupon.dto;

import com.example.jpajavaexample.domain.coupon.model.Coupon;
import com.example.jpajavaexample.domain.coupon.model.FixedAmountCoupon;
import com.example.jpajavaexample.domain.coupon.model.PercentageCoupon;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponDetailResponse(
    Long couponId,
    String code,
    String name,
    String couponType,
    boolean active,
    LocalDateTime expiresAt,
    BigDecimal amount,
    BigDecimal discountRate,
    BigDecimal maxDiscountAmount
) {

    public static CouponDetailResponse from(Coupon coupon) {
        if (coupon instanceof FixedAmountCoupon fixedAmountCoupon) {
            return new CouponDetailResponse(
                fixedAmountCoupon.getId(),
                fixedAmountCoupon.getCode(),
                fixedAmountCoupon.getName(),
                "FIXED",
                fixedAmountCoupon.isActive(),
                fixedAmountCoupon.getExpiresAt(),
                fixedAmountCoupon.getAmount(),
                null,
                null
            );
        }
        if (coupon instanceof PercentageCoupon percentageCoupon) {
            return new CouponDetailResponse(
                percentageCoupon.getId(),
                percentageCoupon.getCode(),
                percentageCoupon.getName(),
                "PERCENT",
                percentageCoupon.isActive(),
                percentageCoupon.getExpiresAt(),
                null,
                percentageCoupon.getDiscountRate(),
                percentageCoupon.getMaxDiscountAmount()
            );
        }
        return new CouponDetailResponse(
            coupon.getId(),
            coupon.getCode(),
            coupon.getName(),
            coupon.getClass().getSimpleName(),
            coupon.isActive(),
            coupon.getExpiresAt(),
            null,
            null,
            null
        );
    }
}
