package com.example.jpajavaexample.domain.reservation.repository;

import com.example.jpajavaexample.domain.Reservation;

public interface ReservationRepository {

    long count();

    Reservation save(Reservation reservation);
}
