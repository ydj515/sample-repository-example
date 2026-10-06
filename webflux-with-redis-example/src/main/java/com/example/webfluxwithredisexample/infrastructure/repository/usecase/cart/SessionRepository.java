package com.example.webfluxwithredisexample.infrastructure.repository.usecase.cart;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SessionRepository {
    private final ReactiveRedisTemplate<String, String> template;
    private static final RedisScript<Long> CREATE = RedisScript.of(
            new ClassPathResource("lua/usecase/cart/create-session.lua"), Long.class
    );
    private static final RedisScript<String> TOUCH = RedisScript.of(
            new ClassPathResource("lua/usecase/cart/touch-session.lua"), String.class
    );

    public Mono<String> create(String user) {
        String id = UUID.randomUUID().toString();
        return template.execute(CREATE, List.of("usecase:session:" + id), user, "1800")
                .single()
                .thenReturn(id);
    }

    public Mono<String> touch(String id) {
        return template.execute(TOUCH, List.of("usecase:session:" + id), "1800").next();
    }
}
