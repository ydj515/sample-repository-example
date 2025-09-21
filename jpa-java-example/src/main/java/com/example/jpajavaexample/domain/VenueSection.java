package com.example.jpajavaexample.domain;

import jakarta.persistence.*;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// 공연장의 구역
@Entity
@Setter
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
    @OneToMany(mappedBy = "venueSection", cascade = CascadeType.ALL)
    private List<Seat> seats = new ArrayList<>();

}
