package com.example.webfluxwithredisexample.infrastructure.repository.usecase.inventory;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StockRepository {
    private final ReactiveRedisTemplate<String, String> template;
    private static final RedisScript<Long> DECREASE = RedisScript.of(
            new ClassPathResource("lua/usecase/inventory/decrease-stock.lua"), Long.class
    );

    public Mono<Boolean> initialize(String key, long stock) {
        return template.opsForValue().set(key, Long.toString(stock), Duration.ofMinutes(10));
    }

    public Mono<Long> get(String key) {
        return template.opsForValue().get(key).map(Long::parseLong);
    }

    public Mono<Boolean> set(String key, long stock) {
        return template.opsForValue().set(key, Long.toString(stock), Duration.ofMinutes(10));
    }

    public Mono<Long> decrement(String key, long quantity) {
        return template.opsForValue().increment(key, -quantity);
    }

    public Mono<Long> guardedDecrement(String key, long quantity) {
        return template.execute(DECREASE, List.of(key), Long.toString(quantity)).single();
    }
}
