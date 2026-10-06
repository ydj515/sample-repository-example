package com.example.webfluxwithredisexample.application.usecase.product;

import com.example.webfluxwithredisexample.domain.usecase.product.Product;
import com.example.webfluxwithredisexample.infrastructure.repository.usecase.product.*;

import lombok.RequiredArgsConstructor;

import org.redisson.api.RedissonReactiveClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ProductCacheService {
    private final ProductCacheRepository cache;

    private final ProductFakeDbRepository db;

    private final RedissonReactiveClient redisson;

    public Mono<Product> get(String id, boolean protectedLoad) {
        return cache.get(id)
                .switchIfEmpty(Mono.defer(() -> protectedLoad ? lockedLoad(id) : load(id)));
    }

    private Mono<Product> load(String id) {
        return db.findById(id)
                .switchIfEmpty(
                        Mono.error(
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Product not found")))
                .flatMap(product -> cache.put(product).thenReturn(product));
    }

    private Mono<Product> lockedLoad(String id) {
        return Mono.defer(
                () -> {
                    var lock = redisson.getLock("usecase:product:lock:" + id);
                    long owner = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
                    return Mono.usingWhen(
                            lock.tryLock(2, 5, TimeUnit.SECONDS, owner)
                                    .flatMap(
                                            acquired ->
                                                    acquired
                                                            ? Mono.just(lock)
                                                            : Mono.error(
                                                                    new ResponseStatusException(
                                                                            HttpStatus
                                                                                    .SERVICE_UNAVAILABLE,
                                                                            "Cache load busy"))),
                            acquired -> cache.get(id).switchIfEmpty(Mono.defer(() -> load(id))),
                            acquired -> acquired.unlock(owner));
                });
    }

    public Mono<Long> ttl(String id) {
        return cache.ttl(id);
    }

    public long dbReads() {
        return db.readCount();
    }
}
