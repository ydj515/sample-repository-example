package com.example.jpajavaexample.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

// 공연장
@Entity
public class Venue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String location;

    // 공연장에 포함된 구역 목록 (ex. 1층, 2층, VIP 구역)
    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL)
    private List<VenueSection> sections = new ArrayList<>();

    public void addSection(VenueSection section) {
        sections.add(section);
        section.setVenue(this);
    }
}