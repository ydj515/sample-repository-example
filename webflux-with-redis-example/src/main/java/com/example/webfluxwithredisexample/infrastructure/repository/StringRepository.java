package com.example.webfluxwithredisexample.infrastructure.repository;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Repository
public class StringRepository {
    private final ReactiveRedisTemplate<String, String> template;
    private final Gson gson;

    @Value("${app.redis.default-ttl}")
    private Duration defaultExpireTime;

    public <T> Mono<T> getData(String key, Class<T> clazz) {
        return template.opsForValue().get(key)
                .map(json -> gson.fromJson(json, clazz));
    }

    public <T> Mono<Boolean> setData(String key, T value) {
        String jsonValue = gson.toJson(value);
        return template.opsForValue().set(key, jsonValue)
                .then(template.expire(key, defaultExpireTime));
    }

    public <T> Mono<Boolean> multiSetData(Map<String, T> datas) {
        Map<String, String> jsonMap = new HashMap<>();
        datas.forEach((k, v) -> jsonMap.put(k, gson.toJson(v)));
        return template.opsForValue().multiSet(jsonMap);
    }

    public Mono<Boolean> setRaw(String key, String value, Duration ttl) {
        if (ttl == null) {
            return template.opsForValue().set(key, value);
        }

        return template.opsForValue().set(key, value, ttl);
    }

    public Mono<String> getRaw(String key) {
        return template.opsForValue().get(key)
                .map(this::deserializeString);
    }

    public Mono<List<String>> multiGetRaw(List<String> keys) {
        return template.opsForValue().multiGet(keys)
                .map(values -> values.stream()
                        .map(this::deserializeString)
                        .toList());
    }

    public Mono<Long> increment(String key, long delta) {
        if (delta == 1L) {
            return template.opsForValue().increment(key);
        }

        return template.opsForValue().increment(key, delta);
    }

    public Mono<Long> decrement(String key, long delta) {
        if (delta == 1L) {
            return template.opsForValue().decrement(key);
        }

        return template.opsForValue().decrement(key, delta);
    }

    public Mono<Duration> getExpire(String key) {
        return template.getExpire(key);
    }

    public Mono<Boolean> setIfAbsent(String key, String value, Duration ttl) {
        if (ttl == null) {
            return template.opsForValue().setIfAbsent(key, value);
        }

        return template.opsForValue().setIfAbsent(key, value, ttl);
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
