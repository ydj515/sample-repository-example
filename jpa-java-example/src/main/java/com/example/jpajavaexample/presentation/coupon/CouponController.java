package com.example.jpajavaexample.presentation.coupon;

import com.example.jpajavaexample.application.coupon.CouponQueryService;
import com.example.jpajavaexample.presentation.coupon.dto.CouponDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponQueryService couponQueryService;

    @GetMapping("/{code}")
    public ResponseEntity<CouponDetailResponse> getCouponByCode(@PathVariable String code) {
        CouponDetailResponse response = couponQueryService.getCouponByCode(code);
        return ResponseEntity.ok(response);
    }
}
