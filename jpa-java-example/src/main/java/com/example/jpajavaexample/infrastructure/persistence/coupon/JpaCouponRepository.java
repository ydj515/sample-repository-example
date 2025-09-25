package com.example.jpajavaexample.infrastructure.persistence.coupon;

import com.example.jpajavaexample.domain.coupon.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCouponRepository extends JpaRepository<Coupon, Long> {
}
