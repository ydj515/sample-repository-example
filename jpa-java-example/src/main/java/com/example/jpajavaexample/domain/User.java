package com.example.jpajavaexample.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

// 사용자
@Entity
@Table(name = "users") // 'USER'는 DB 예약어인 경우가 많아 'users'로 테이블명 지정
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    // 사용자의 예매 목록
    @OneToMany(mappedBy = "user")
    private List<Reservation> reservations = new ArrayList<>();
}