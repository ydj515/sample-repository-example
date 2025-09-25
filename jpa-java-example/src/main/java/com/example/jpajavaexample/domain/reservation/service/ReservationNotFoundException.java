package com.example.jpajavaexample.domain.reservation.service;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(Long reservationId) {
        super("Reservation not found. id=" + reservationId);
    }
}
