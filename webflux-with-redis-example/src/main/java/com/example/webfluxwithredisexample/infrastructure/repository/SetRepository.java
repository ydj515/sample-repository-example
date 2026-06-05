package com.example.webfluxwithredisexample.infrastructure.repository;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Repository
public class SetRepository {
    private final ReactiveRedisTemplate<String, String> template;
    private final Gson gson;

    public Mono<Long> add(String key, List<String> members) {
        return template.opsForSet().add(key, members.stream()
                .map(gson::toJson)
                .toArray(String[]::new));
    }

    public Mono<Long> remove(String key, List<String> members) {
        return template.opsForSet().remove(key, members.stream()
                .map(gson::toJson)
                .toArray(Object[]::new));
    }

    public Flux<String> members(String key) {
        return template.opsForSet().members(key)
                .map(this::deserializeString);
    }

    public Mono<Boolean> isMember(String key, String member) {
        return template.opsForSet().isMember(key, gson.toJson(member));
    }

    public Mono<Long> size(String key) {
        return template.opsForSet().size(key);
    }

    public Mono<String> pop(String key) {
        return template.opsForSet().pop(key)
                .map(this::deserializeString);
    }

    public Flux<String> pop(String key, long count) {
        return template.opsForSet().pop(key, count)
                .map(this::deserializeString);
    }

    public Mono<String> randomMember(String key) {
        return template.opsForSet().randomMember(key)
                .map(this::deserializeString);
    }

    public Flux<String> randomMembers(String key, long count, boolean distinct) {
        Flux<String> members = distinct
                ? template.opsForSet().distinctRandomMembers(key, count)
                : template.opsForSet().randomMembers(key, count);

        return members.map(this::deserializeString);
    }

    public Flux<String> intersect(List<String> keys) {
        return template.opsForSet().intersect(keys)
                .map(this::deserializeString);
    }

    public Flux<String> union(List<String> keys) {
        return template.opsForSet().union(keys)
                .map(this::deserializeString);
    }

    public Flux<String> difference(List<String> keys) {
        return template.opsForSet().difference(keys)
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
