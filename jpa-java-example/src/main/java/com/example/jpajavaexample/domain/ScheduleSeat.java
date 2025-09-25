package com.example.jpajavaexample.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회차별 좌석 정보 (상태, 가격 등)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    private Seat seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Enumerated(EnumType.STRING)
    private SeatStatus status; // AVAILABLE, RESERVED, CONFIRMED

    private BigDecimal price;

    @Version // 동시성 제어를 위한 버전
    private Long version;

    private ScheduleSeat(Schedule schedule, Seat seat, BigDecimal price, SeatStatus status) {
        this.schedule = schedule;
        this.seat = seat;
        this.price = price;
        this.status = status;
    }

    public static ScheduleSeat createAvailable(Schedule schedule, Seat seat, BigDecimal price) {
        return new ScheduleSeat(schedule, seat, price, SeatStatus.AVAILABLE);
    }

    public void reserveFor(Reservation reservation) {
        this.reservation = reservation;
        this.status = SeatStatus.RESERVED;
        reservation.addReservedSeat(this);
    }
}
