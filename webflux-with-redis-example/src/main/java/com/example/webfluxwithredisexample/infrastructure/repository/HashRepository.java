package com.example.webfluxwithredisexample.infrastructure.repository;

import com.example.webfluxwithredisexample.domain.HashModel;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Repository
public class HashRepository {
    private final ReactiveRedisTemplate<String, String> template;
    private final Gson gson;

    @Value("${app.redis.default-ttl}")
    private Duration defaultExpireTime;

    public <T> Mono<Boolean> putInHash(String key, String field, T value) {
        return template.opsForHash().put(key, field, gson.toJson(value))
                .delayUntil(result -> template.expire(key, defaultExpireTime));
    }

    public <T> Mono<T> getFromHash(String key, String field, Class<T> clazz) {
        return template.opsForHash().get(key, field)
                .map(value -> {
                    try {
                        return gson.fromJson(value.toString(), clazz);
                    } catch (JsonSyntaxException e) {
                        // 만약 JSON이 아니라 그냥 String이면, 직접 감싸서 HashModel로 만들어서 리턴
                        if (clazz.equals(HashModel.class)) {
                            return clazz.cast(new HashModel(value.toString()));
                        }
                        throw e;
                    }
                });
    }

    public Mono<Long> removeFromHash(String key, String field) {
        return template.opsForHash().remove(key, field);
    }

    public Mono<List<String>> multiGet(String key, List<String> fields) {
        return template.opsForHash().multiGet(key, (java.util.Collection<Object>) (java.util.Collection<?>) fields)
                .map(values -> values.stream()
                        .map(value -> deserializeString(value == null ? null : value.toString()))
                        .toList());
    }

    public Mono<Map<String, String>> getEntries(String key) {
        return template.opsForHash().entries(key)
                .collectMap(
                        entry -> entry.getKey().toString(),
                        entry -> deserializeString(entry.getValue() == null ? null : entry.getValue().toString()),
                        LinkedHashMap::new
                );
    }

    public Mono<Boolean> exists(String key, String field) {
        return template.opsForHash().hasKey(key, field);
    }

    public Mono<List<String>> getKeys(String key) {
        return template.opsForHash().keys(key)
                .map(Object::toString)
                .collectList();
    }

    public Mono<List<String>> getValues(String key) {
        return template.opsForHash().values(key)
                .map(value -> deserializeString(value == null ? null : value.toString()))
                .collectList();
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
