package com.example.jpajavaexample.application.reservation;

import com.example.jpajavaexample.domain.Reservation;
import com.example.jpajavaexample.domain.coupon.model.Coupon;
import com.example.jpajavaexample.domain.coupon.repository.CouponRepository;
import com.example.jpajavaexample.domain.coupon.service.CouponNotFoundException;
import com.example.jpajavaexample.domain.reservation.repository.ReservationRepository;
import com.example.jpajavaexample.domain.reservation.service.ReservationNotFoundException;
import com.example.jpajavaexample.presentation.reservation.dto.ReservationPricingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationCouponService {

    private final ReservationRepository reservationRepository;
    private final CouponRepository couponRepository;

    @Transactional
    public ReservationPricingResponse applyCoupon(Long reservationId, Long couponId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new CouponNotFoundException(couponId));

        reservation.applyCoupon(coupon);
        reservationRepository.save(reservation);

        return ReservationPricingResponse.from(reservation);
    }

    @Transactional
    public ReservationPricingResponse removeCoupon(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        reservation.removeCoupon();
        reservationRepository.save(reservation);

        return ReservationPricingResponse.from(reservation);
    }
}
