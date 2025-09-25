package com.example.jpajavaexample.infrastructure.persistence.coupon;

import com.example.jpajavaexample.domain.coupon.model.Coupon;
import com.example.jpajavaexample.domain.coupon.repository.CouponRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {

    private final JpaCouponRepository jpaCouponRepository;

    @Override
    @Transactional
    public Coupon save(Coupon coupon) {
        return jpaCouponRepository.save(coupon);
    }

    @Override
    public Optional<Coupon> findById(Long id) {
        return jpaCouponRepository.findById(id);
    }

    @Override
    public List<Coupon> findAll() {
        return jpaCouponRepository.findAll();
    }

    @Override
    public long count() {
        return jpaCouponRepository.count();
    }
}
