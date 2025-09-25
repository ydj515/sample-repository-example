package com.example.jpajavaexample.infrastructure.persistence.reservation;

import com.example.jpajavaexample.domain.Reservation;
import com.example.jpajavaexample.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReservationRepositoryImpl implements ReservationRepository {

    private final JpaReservationRepository jpaReservationRepository;

    @Override
    public long count() {
        return jpaReservationRepository.count();
    }

    @Override
    @Transactional
    public Reservation save(Reservation reservation) {
        return jpaReservationRepository.save(reservation);
    }
}
