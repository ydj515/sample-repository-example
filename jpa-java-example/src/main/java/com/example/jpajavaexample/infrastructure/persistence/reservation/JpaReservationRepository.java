package com.example.jpajavaexample.infrastructure.persistence.reservation;

import com.example.jpajavaexample.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaReservationRepository extends JpaRepository<Reservation, Long> {
}
