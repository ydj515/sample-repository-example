package com.example.jpajavaexample.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

// 좌석
@Entity
public class Seat {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private VenueSection venueSection; // 공연장의 구역(ex. R석, S석)

    private String rowNumber;
    private int seatNumber;
}
