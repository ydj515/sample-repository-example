package com.example.jpajavaexample.domain.coupon.model;

import com.example.jpajavaexample.domain.coupon.service.CouponUsageContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.DiscriminatorValue;

@Entity
@DiscriminatorValue("FIXED")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FixedAmountCoupon extends Coupon {

    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    private FixedAmountCoupon(String code, String name, boolean active, LocalDateTime expiresAt, BigDecimal amount) {
        super(code, name, active, expiresAt);
        this.amount = amount == null ? BigDecimal.ZERO : amount.max(BigDecimal.ZERO);
    }

    public static FixedAmountCoupon create(String code, String name, BigDecimal amount) {
        return new FixedAmountCoupon(code, name, true, null, amount);
    }

    public static FixedAmountCoupon create(String code, String name, boolean active, LocalDateTime expiresAt, BigDecimal amount) {
        return new FixedAmountCoupon(code, name, active, expiresAt, amount);
    }

    @Override
    protected BigDecimal doCalculateDiscount(CouponUsageContext context) {
        return amount;
    }
}
