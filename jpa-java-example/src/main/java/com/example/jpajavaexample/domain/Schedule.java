package com.example.jpajavaexample.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import com.example.jpajavaexample.domain.performance.model.Performance;

import java.time.LocalDateTime;

// 회차 (ex. '오페라의 유령'의 '9월 1일 19:00')
@Entity
public class Schedule {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Performance performance;

    @ManyToOne(fetch = FetchType.LAZY)
    private Venue venue; // 공연장 정보

    private LocalDateTime startsAt; // 공연 시작 시간
}
