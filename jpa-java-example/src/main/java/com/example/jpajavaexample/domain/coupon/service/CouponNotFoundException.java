package com.example.jpajavaexample.domain.coupon.service;

public class CouponNotFoundException extends RuntimeException {

    public CouponNotFoundException(Long couponId) {
        super("Coupon not found. id=" + couponId);
    }
}
