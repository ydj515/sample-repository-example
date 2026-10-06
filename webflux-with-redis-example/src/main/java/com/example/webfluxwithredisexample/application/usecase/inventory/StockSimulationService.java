package com.example.webfluxwithredisexample.application.usecase.inventory;

import com.example.webfluxwithredisexample.infrastructure.repository.usecase.inventory.StockRepository;
import com.example.webfluxwithredisexample.presentation.router.usecase.inventory.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.*;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockSimulationService {
    private final StockRepository stock;

    private final InventoryService inventory;

    public Mono<StockSimulationResponse> run(StockSimulationRequest request) {
        if (request.mode() == null
                || !Set.of("unsafe", "atomic", "lock", "lua").contains(request.mode())
                || request.initialStock() < 0
                || request.initialStock() > 1000000
                || request.requests() < 1
                || request.requests() > 1000
                || request.concurrency() < 1
                || request.concurrency() > 100
                || request.quantity() < 1
                || request.quantity() > 1000000)
            return Mono.error(new IllegalArgumentException("Invalid simulation bounds or mode"));
        return Mono.defer(
                () -> {
                    String run = UUID.randomUUID().toString();
                    String key = "usecase:stock:" + run;
                    return stock.initialize(key, request.initialStock())
                            .thenMany(
                                    Flux.range(0, request.requests())
                                            .flatMap(
                                                    index ->
                                                            inventory.decrease(
                                                                    key,
                                                                    request.mode(),
                                                                    request.quantity()),
                                                    request.concurrency()))
                            .filter(result -> result.accepted())
                            .count()
                            .flatMap(
                                    accepted ->
                                            stock.get(key)
                                                    .map(
                                                            remaining -> {
                                                                long expected =
                                                                        request.initialStock()
                                                                                - accepted
                                                                                        * request
                                                                                                .quantity();
                                                                return new StockSimulationResponse(
                                                                        run,
                                                                        request.mode(),
                                                                        request.initialStock(),
                                                                        request.requests(),
                                                                        accepted,
                                                                        request.requests()
                                                                                - accepted,
                                                                        remaining,
                                                                        expected,
                                                                        remaining >= 0
                                                                                && remaining
                                                                                        == expected);
                                                            }));
                });
    }
}
