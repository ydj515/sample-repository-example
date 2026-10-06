package com.example.webfluxwithredisexample.application.usecase.inventory;

import com.example.webfluxwithredisexample.domain.usecase.inventory.StockResult;
import com.example.webfluxwithredisexample.infrastructure.repository.usecase.inventory.StockRepository;

import lombok.RequiredArgsConstructor;

import org.redisson.api.RedissonReactiveClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final StockRepository stock;

    private final RedissonReactiveClient redisson;

    public Mono<StockResult> decrease(String key, String mode, long quantity) {
        if (quantity < 1) {
            return Mono.error(new IllegalArgumentException("quantity must be positive"));
        }
        return switch (mode) {
            case "unsafe" -> readThenWrite(key, quantity, true);
            case "atomic" ->
                    stock.decrement(key, quantity).map(left -> new StockResult(left >= 0, left));
            case "lua" ->
                    stock.guardedDecrement(key, quantity)
                            .map(left -> new StockResult(left >= 0, left));
            case "lock" ->
                    Mono.defer(
                            () -> {
                                var lock = redisson.getLock(key + ":lock");
                                long owner =
                                        ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
                                return Mono.usingWhen(
                                        lock.tryLock(5, 10, TimeUnit.SECONDS, owner)
                                                .flatMap(
                                                        acquired ->
                                                                acquired
                                                                        ? Mono.just(lock)
                                                                        : Mono.error(
                                                                                new ResponseStatusException(
                                                                                        HttpStatus
                                                                                                .SERVICE_UNAVAILABLE,
                                                                                        "Stock lock"
                                                                                            + " busy"))),
                                        acquired -> readThenWrite(key, quantity, false),
                                        acquired -> acquired.unlock(owner));
                            });
            default ->
                    Mono.error(
                            new IllegalArgumentException(
                                    "mode must be unsafe, atomic, lock or lua"));
        };
    }

    private Mono<StockResult> readThenWrite(String key, long quantity, boolean delay) {
        return stock.get(key)
                .flatMap(
                        current -> {
                            if (current < quantity) {
                                return Mono.just(new StockResult(false, current));
                            }
                            Mono<Long> observed = Mono.just(current);
                            if (delay) {
                                observed = observed.delayElement(Duration.ofMillis(20));
                            }
                            return observed.flatMap(
                                    value ->
                                            stock.set(key, value - quantity)
                                                    .thenReturn(
                                                            new StockResult(
                                                                    true, value - quantity)));
                        });
    }
}
