package com.example.webfluxwithredisexample.application.usecase.orderevent;

import com.example.webfluxwithredisexample.domain.usecase.orderevent.OrderEvent;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class OrderEventProcessor {
    // 샘플 후처리 결과를 반환한다. 실제 외부 시스템에 전송하지 않는다.
    public Mono<OrderEvent> process(OrderEvent event) {
        return Mono.just(event);
    }
}
