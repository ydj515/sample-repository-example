package com.example.jpajavaexample.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

// 회차별 좌석 정보 (상태, 가격 등)
@Entity
public class ScheduleSeat {
    @Id
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
}
