package com.example.jpajavaexample.domain.coupon.model;

import com.example.jpajavaexample.domain.coupon.service.CouponUsageContext;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("PERCENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PercentageCoupon extends Coupon {

    @Column(precision = 5, scale = 4)
    private BigDecimal discountRate;

    @Column(precision = 18, scale = 2)
    private BigDecimal maxDiscountAmount;

    private PercentageCoupon(String code, String name, boolean active, LocalDateTime expiresAt, BigDecimal discountRate, BigDecimal maxDiscountAmount) {
        super(code, name, active, expiresAt);
        this.discountRate = discountRate;
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public static PercentageCoupon create(String code, String name, BigDecimal discountRate) {
        return new PercentageCoupon(code, name, true, null, sanitizeRate(discountRate), BigDecimal.valueOf(100_000));
    }

    public static PercentageCoupon create(String code, String name, boolean active, LocalDateTime expiresAt, BigDecimal discountRate, BigDecimal maxDiscountAmount) {
        return new PercentageCoupon(code, name, active, expiresAt, sanitizeRate(discountRate), sanitizeAmount(maxDiscountAmount));
    }

    @Override
    protected BigDecimal doCalculateDiscount(CouponUsageContext context) {
        BigDecimal discount = context.totalPrice().multiply(discountRate)
            .setScale(0, RoundingMode.DOWN);
        return discount.min(maxDiscountAmount);
    }

    private static BigDecimal sanitizeRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (rate.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return rate;
    }

    private static BigDecimal sanitizeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return amount;
    }
}
