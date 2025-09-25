package com.example.jpajavaexample.application.coupon;

import com.example.jpajavaexample.domain.coupon.model.Coupon;
import com.example.jpajavaexample.domain.coupon.repository.CouponRepository;
import com.example.jpajavaexample.domain.coupon.service.CouponNotFoundException;
import com.example.jpajavaexample.presentation.coupon.dto.CouponDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponQueryService {

    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public CouponDetailResponse getCouponByCode(String code) {
        Coupon coupon = couponRepository.findByCode(code)
            .orElseThrow(() -> new CouponNotFoundException(code));

        return CouponDetailResponse.from(coupon);
    }
}
