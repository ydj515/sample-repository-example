package com.example.webfluxwithredisexample.infrastructure.repository;

import com.example.webfluxwithredisexample.domain.ValueWithTTL;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReactiveRedisRepository {
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

    public <T> Mono<Boolean> addToSortedSet(String key, T value, Double score) {
        String jsonValue = gson.toJson(value);
        return template.opsForZSet().add(key, jsonValue, score);
    }

    public <T> Flux<T> rangeByScore(String key, double minScore, double maxScore, Class<T> clazz) {
        return template.opsForZSet().rangeByScore(key, Range.closed(minScore, maxScore))
                .map(json -> gson.fromJson(json, clazz));
    }

    public <T> Flux<T> getTopNFromSortedSet(String key, int n, Class<T> clazz) {
        return template.opsForZSet().reverseRange(key, Range.closed(0L, (long) n - 1))
                .map(json -> gson.fromJson(json, clazz));
    }

    public <T> Mono<Long> addToListLeft(String key, T value) {
        return template.opsForList().leftPush(key, gson.toJson(value));
    }

    public <T> Mono<Long> addToListRight(String key, T value) {
        return template.opsForList().rightPush(key, gson.toJson(value));
    }

    public <T> Flux<T> getAllList(String key, Class<T> clazz) {
        return template.opsForList().range(key, 0, -1)
                .map(json -> gson.fromJson(json, clazz));
    }

    public <T> Mono<Long> removeFromList(String key, T value) {
        return template.opsForList().remove(key, 1, gson.toJson(value));
    }

    public <T> Mono<Boolean> putInHash(String key, String field, T value) {
        return template.opsForHash().put(key, field, gson.toJson(value));
    }

    public <T> Mono<T> getFromHash(String key, String field, Class<T> clazz) {
        return template.opsForHash().get(key, field)
                .map(obj -> gson.fromJson(obj.toString(), clazz));
    }

    public Mono<Long> removeFromHash(String key, String field) {
        return template.opsForHash().remove(key, field);
    }

    public Mono<Boolean> setBit(String key, long offset, boolean value) {
        return template.opsForValue().setBit(key, offset, value);
    }

    public Mono<Boolean> getBit(String key, long offset) {
        return template.opsForValue().getBit(key, offset);
    }

    public <T> Mono<ValueWithTTL<T>> getValueWithTtl(String key, Class<T> clazz) {
        return template.opsForValue().get(key)
                .zipWith(template.getExpire(key))
                .map(tuple -> new ValueWithTTL<>(gson.fromJson(tuple.getT1(), clazz), tuple.getT2()));
    }

    public Flux<Long> sumTwoKeysAndRenew(String key1, String key2, String resultKey) {
        RedisScript<Long> script = RedisScript.of(new ClassPathResource("/lua/newKey.lua"), Long.class);
        return template.execute(script, List.of(key1, key2, resultKey));
    }

}

