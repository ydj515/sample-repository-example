package com.example.jpajavaexample.domain.coupon.model;

import com.example.jpajavaexample.domain.coupon.service.CouponUsageContext;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coupons")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "coupon_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Column
    private LocalDateTime expiresAt;

    protected Coupon(String code, String name, boolean active, LocalDateTime expiresAt) {
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.active = active;
        this.expiresAt = expiresAt;
    }

    public BigDecimal calculateDiscount(CouponUsageContext context) {
        if (!isUsable(context.usedAt())) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = doCalculateDiscount(context);
        if (discount == null || discount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return discount.min(context.totalPrice());
    }

    protected abstract BigDecimal doCalculateDiscount(CouponUsageContext context);

    private boolean isUsable(LocalDateTime usedAt) {
        if (!active) {
            return false;
        }
        if (expiresAt == null) {
            return true;
        }
        LocalDateTime evaluateAt = usedAt != null ? usedAt : LocalDateTime.now();
        return !expiresAt.isBefore(evaluateAt);
    }
}
