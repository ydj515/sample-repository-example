package com.example.jpajavaexample.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

// 공연 (ex. '오페라의 유령')
@Entity
public class Performance {
    @Id
    private Long id;
    private String title;
    private String category;
}
