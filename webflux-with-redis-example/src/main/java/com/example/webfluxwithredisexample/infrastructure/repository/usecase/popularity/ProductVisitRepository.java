package com.example.webfluxwithredisexample.infrastructure.repository.usecase.popularity;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Repository
@RequiredArgsConstructor
public class ProductVisitRepository {
    private final ReactiveRedisTemplate<String, String> template;

    public Mono<Long> record(String product, long visitor) {
        String day = LocalDate.now(ZoneOffset.UTC).toString();
        String prefix = "usecase:visits:" + product + ":" + day;

        return template.opsForHyperLogLog()
                .add(prefix + ":hll", Long.toString(visitor))
                .then(template.opsForValue().setBit(prefix + ":bitmap", visitor, true))
                .then(template.expire(prefix + ":hll", Duration.ofDays(7)))
                .then(template.expire(prefix + ":bitmap", Duration.ofDays(7)))
                .then(template.opsForHyperLogLog().size(prefix + ":hll"));
    }

    public Mono<Boolean> visited(String product, long visitor) {
        String day = LocalDate.now(ZoneOffset.UTC).toString();

        return template.opsForValue()
                .getBit("usecase:visits:" + product + ":" + day + ":bitmap", visitor);
    }
}
