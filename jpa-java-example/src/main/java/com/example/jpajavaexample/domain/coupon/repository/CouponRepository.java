package com.example.jpajavaexample.domain.coupon.repository;

import com.example.jpajavaexample.domain.coupon.model.Coupon;
import java.util.List;
import java.util.Optional;

public interface CouponRepository {

    Coupon save(Coupon coupon);

    Optional<Coupon> findById(Long id);

    Optional<Coupon> findByCode(String code);

    List<Coupon> findAll();

    long count();
}
