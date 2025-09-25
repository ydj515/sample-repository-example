package com.example.jpajavaexample.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 좌석
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private VenueSection venueSection; // 공연장의 구역(ex. R석, S석)

    @Column(name = "row_no")
    private String rowNumber;

    @Column(name = "seat_no")
    private int seatNumber;

    private Seat(VenueSection venueSection, String rowNumber, int seatNumber) {
        this.venueSection = venueSection;
        this.rowNumber = rowNumber;
        this.seatNumber = seatNumber;
    }

    public static Seat create(VenueSection venueSection, String rowNumber, int seatNumber) {
        return new Seat(venueSection, rowNumber, seatNumber);
    }
}
