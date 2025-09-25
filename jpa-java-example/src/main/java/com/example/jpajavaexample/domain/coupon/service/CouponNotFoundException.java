package com.example.jpajavaexample.domain.coupon.service;

public class CouponNotFoundException extends RuntimeException {

    public CouponNotFoundException(Long couponId) {
        super("Coupon not found. id=" + couponId);
    }

    public CouponNotFoundException(String code) {
        super("Coupon not found. code=" + code);
    }
}
