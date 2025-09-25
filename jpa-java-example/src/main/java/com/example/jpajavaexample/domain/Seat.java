package com.example.jpajavaexample.domain;

import jakarta.persistence.*;

// 좌석
@Entity
public class Seat {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private VenueSection venueSection; // 공연장의 구역(ex. R석, S석)

    @Column(name = "row_no")
    private String rowNumber;
    @Column(name = "seat_no")
    private int seatNumber;
}
