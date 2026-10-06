package com.example.webfluxwithredisexample.infrastructure.repository.usecase.popularity;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class WishlistRepository {
    private final ReactiveRedisTemplate<String, String> template;

    public Mono<Long> add(String user, String product) {
        return template.opsForSet().add("usecase:wishlist:" + user, product);
    }

    public Flux<String> members(String user) {
        return template.opsForSet().members("usecase:wishlist:" + user);
    }
}
