package com.example.jpajavaexample.domain;

import com.example.jpajavaexample.domain.coupon.model.Coupon;
import com.example.jpajavaexample.domain.coupon.service.CouponUsageContext;
import com.example.jpajavaexample.domain.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 예매
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Schedule schedule;

    private LocalDateTime reservedAt;

    // 이 예매에 포함된 좌석들
    @OneToMany(mappedBy = "reservation")
    private final List<ScheduleSeat> reservedSeats = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    private Coupon coupon;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount;

    private Reservation(User user, Schedule schedule, LocalDateTime reservedAt) {
        this.user = user;
        this.schedule = schedule;
        this.reservedAt = reservedAt;
        this.discountAmount = BigDecimal.ZERO;
    }

    public static Reservation create(User user, Schedule schedule, LocalDateTime reservedAt) {
        return new Reservation(user, schedule, reservedAt);
    }

    void addReservedSeat(ScheduleSeat scheduleSeat) {
        reservedSeats.add(scheduleSeat);
    }

    public BigDecimal calculateTotalPrice() {
        return reservedSeats.stream()
            .map(ScheduleSeat::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateFinalPrice() {
        BigDecimal total = calculateTotalPrice();
        BigDecimal result = total.subtract(discountAmount);
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }

    public void applyCoupon(Coupon coupon) {
        this.coupon = coupon;
        if (coupon == null) {
            this.discountAmount = BigDecimal.ZERO;
            return;
        }

        CouponUsageContext context = new CouponUsageContext(
            calculateTotalPrice(),
            buildSeatGradeCounts(),
            reservedAt
        );

        this.discountAmount = coupon.calculateDiscount(context);
    }

    public void removeCoupon() {
        this.coupon = null;
        this.discountAmount = BigDecimal.ZERO;
    }

    private Map<String, Long> buildSeatGradeCounts() {
        return reservedSeats.stream()
            .collect(Collectors.groupingBy(
                seat -> seat.getSeat().getVenueSection().getName(),
                Collectors.counting()
            ));
    }
}
