package com.example.webfluxwithredisexample.application;

import com.example.webfluxwithredisexample.infrastructure.repository.HashRepository;
import com.example.webfluxwithredisexample.presentation.router.hash.HashReqeust;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HashAsyncService {

    private final HashRepository redis;

    public <T> Mono<Boolean> putInHash(HashReqeust hashReqeust) {

        return redis.putInHash(hashReqeust.baseRequest().key(), hashReqeust.field(), hashReqeust.name());
    }

    public <T> Mono<T> getFromHash(String key, String field, Class<T> clazz) {
        return redis.getFromHash(key, field, clazz);
    }

    public Mono<List<String>> multiGet(String key, List<String> fields) {
        return redis.multiGet(key, fields);
    }

    public Mono<Map<String, String>> getEntries(String key) {
        return redis.getEntries(key);
    }

    public Mono<Boolean> exists(String key, String field) {
        return redis.exists(key, field);
    }

    public Mono<List<String>> getKeys(String key) {
        return redis.getKeys(key);
    }

    public Mono<List<String>> getValues(String key) {
        return redis.getValues(key);
    }

}
