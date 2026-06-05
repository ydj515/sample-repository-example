package com.example.webfluxwithredisexample.infrastructure.repository;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Repository
public class ListRepository {
    private final ReactiveRedisTemplate<String, String> template;
    private final Gson gson;

    @Value("${app.redis.default-ttl}")
    private Duration defaultExpireTime;

    public <T> Mono<Long> addToListLeft(String key, T value) {
        return template.opsForList().leftPush(key, gson.toJson(value))
                .delayUntil(result -> template.expire(key, defaultExpireTime));
    }

    public <T> Mono<Long> addToListRight(String key, T value) {
        return template.opsForList().rightPush(key, gson.toJson(value))
                .delayUntil(result -> template.expire(key, defaultExpireTime));
    }

    public <T> Flux<T> getAllList(String key, Class<T> clazz) {
        return template.opsForList().range(key, 0, -1)
                .map(json -> gson.fromJson(json, clazz));
    }

    public <T> Mono<Long> removeFromList(String key, T value) {
        return template.opsForList().remove(key, 1, gson.toJson(value));
    }

    public Mono<String> leftPop(String key) {
        return template.opsForList().leftPop(key)
                .map(this::deserializeString);
    }

    public Mono<List<String>> leftPop(String key, long count) {
        return template.opsForList().leftPop(key, count)
                .map(this::deserializeString)
                .collectList();
    }

    public Mono<String> leftPop(String key, Duration timeout) {
        return template.opsForList().leftPop(key, timeout)
                .map(this::deserializeString);
    }

    public Mono<String> rightPop(String key) {
        return template.opsForList().rightPop(key)
                .map(this::deserializeString);
    }

    public Mono<List<String>> rightPop(String key, long count) {
        return template.opsForList().rightPop(key, count)
                .map(this::deserializeString)
                .collectList();
    }

    public Mono<Long> size(String key) {
        return template.opsForList().size(key);
    }

    public Mono<String> index(String key, long index) {
        return template.opsForList().index(key, index)
                .map(this::deserializeString);
    }

    private String deserializeString(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        try {
            return gson.fromJson(rawValue, String.class);
        } catch (JsonSyntaxException e) {
            return rawValue;
        }
    }

}
