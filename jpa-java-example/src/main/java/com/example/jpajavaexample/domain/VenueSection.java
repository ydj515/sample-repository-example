package com.example.jpajavaexample.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 공연장의 구역
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VenueSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    private String name; // ex. VIP석, R석, S석
    private int capacity; // 총 좌석 수

    // 이 구역에 포함된 좌석들
    @OneToMany(mappedBy = "venueSection", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Seat> seats = new ArrayList<>();

    private VenueSection(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
    }

    public static VenueSection create(String name, int capacity) {
        return new VenueSection(name, capacity);
    }

    void setVenue(Venue venue) {
        this.venue = venue;
    }

    public Seat addSeat(String rowNumber, int seatNumber) {
        Seat seat = Seat.create(this, rowNumber, seatNumber);
        seats.add(seat);
        return seat;
    }
}
