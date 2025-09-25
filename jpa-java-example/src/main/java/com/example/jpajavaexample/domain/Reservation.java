package com.example.jpajavaexample.domain;

import com.example.jpajavaexample.domain.user.model.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 예매
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Schedule schedule;

    private LocalDateTime reservedAt;

    // 이 예매에 포함된 좌석들
    @OneToMany(mappedBy = "reservation")
    private final List<ScheduleSeat> reservedSeats = new ArrayList<>();

    private Reservation(User user, Schedule schedule, LocalDateTime reservedAt) {
        this.user = user;
        this.schedule = schedule;
        this.reservedAt = reservedAt;
    }

    public static Reservation create(User user, Schedule schedule, LocalDateTime reservedAt) {
        return new Reservation(user, schedule, reservedAt);
    }

    void addReservedSeat(ScheduleSeat scheduleSeat) {
        reservedSeats.add(scheduleSeat);
    }
}
