package com.example.jpajavaexample.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 공연장
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String location;

    // 공연장에 포함된 구역 목록 (ex. 1층, 2층, VIP 구역)
    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<VenueSection> sections = new ArrayList<>();

    private Venue(String name, String location) {
        this.name = name;
        this.location = location;
    }

    public static Venue create(String name, String location) {
        return new Venue(name, location);
    }

    public VenueSection addSection(String name, int capacity) {
        VenueSection section = VenueSection.create(name, capacity);
        section.setVenue(this);
        sections.add(section);
        return section;
    }
}
