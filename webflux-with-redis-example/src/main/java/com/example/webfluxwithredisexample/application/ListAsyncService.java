package com.example.webfluxwithredisexample.application;

import com.example.webfluxwithredisexample.infrastructure.repository.ListRepository;
import com.example.webfluxwithredisexample.presentation.router.list.ListRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListAsyncService {
    private final ListRepository redis;

    public <T> Mono<Long> addToListLeft(ListRequest req) {
        return redis.addToListLeft(req.baseRequest().key(), req.name());
    }

    public <T> Mono<Long> addToListRight(ListRequest req) {
        return redis.addToListRight(req.baseRequest().key(), req.name());
    }

    public <T> Flux<T> getAllList(String key, Class<T> clazz) {
        return redis.getAllList(key, clazz);
    }

    public <T> Mono<Long> removeFromList(String key, T value) {
        return redis.removeFromList(key, value);
    }

    public Mono<String> leftPop(String key) {
        return redis.leftPop(key);
    }

    public Mono<List<String>> leftPop(String key, long count) {
        return redis.leftPop(key, count);
    }

    public Mono<String> leftPopBlocking(String key, long timeoutSeconds) {
        return redis.leftPop(key, Duration.ofSeconds(timeoutSeconds));
    }

    public Mono<String> rightPop(String key) {
        return redis.rightPop(key);
    }

    public Mono<List<String>> rightPop(String key, long count) {
        return redis.rightPop(key, count);
    }

    public Mono<Long> size(String key) {
        return redis.size(key);
    }

    public Mono<String> index(String key, long index) {
        return redis.index(key, index);
    }
}
