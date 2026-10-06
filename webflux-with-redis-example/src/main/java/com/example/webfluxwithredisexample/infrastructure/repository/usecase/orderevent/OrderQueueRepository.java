package com.example.webfluxwithredisexample.infrastructure.repository.usecase.orderevent;

import com.example.webfluxwithredisexample.domain.usecase.orderevent.OrderEvent;
import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.*;

@Repository
@RequiredArgsConstructor
public class OrderQueueRepository {
    private final ReactiveRedisTemplate<String, String> template;
    private final Gson gson;

    public Mono<Long> publish(OrderEvent event) {
        return template.opsForList().rightPush("usecase:orders:queue", gson.toJson(event));
    }

    public Mono<OrderEvent> next() {
        return template.opsForList()
                .leftPop("usecase:orders:queue")
                .map(json -> gson.fromJson(json, OrderEvent.class));
    }
}
