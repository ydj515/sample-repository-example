package com.example.jpajavaexample.domain;

import com.example.jpajavaexample.domain.user.model.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 예매
@Entity
public class Reservation {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Schedule schedule;

    private LocalDateTime reservedAt;

    // 이 예매에 포함된 좌석들
    @OneToMany(mappedBy = "reservation")
    private List<ScheduleSeat> reservedSeats = new ArrayList<>();
}
