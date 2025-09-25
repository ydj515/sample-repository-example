package com.example.jpajavaexample.domain;

import com.example.jpajavaexample.domain.performance.model.Performance;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회차 (ex. '오페라의 유령'의 '9월 1일 19:00')
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Performance performance;

    @ManyToOne(fetch = FetchType.LAZY)
    private Venue venue; // 공연장 정보

    private LocalDateTime startsAt; // 공연 시작 시간

    private Schedule(Performance performance, Venue venue, LocalDateTime startsAt) {
        this.performance = performance;
        this.venue = venue;
        this.startsAt = startsAt;
    }

    public static Schedule create(Performance performance, Venue venue, LocalDateTime startsAt) {
        return new Schedule(performance, venue, startsAt);
    }
}
