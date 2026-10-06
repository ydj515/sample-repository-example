package com.example.webfluxwithredisexample.presentation.router.usecase.inventory;

import static com.example.webfluxwithredisexample.presentation.router.usecase.UsecaseResponses.ok;

import com.example.webfluxwithredisexample.application.usecase.inventory.StockSimulationService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;

import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class InventoryHandler {
    private final StockSimulationService service;

    public Mono<ServerResponse> simulate(ServerRequest req) {
        return ok(
                req.bodyToMono(StockSimulationRequest.class)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Body required")))
                        .flatMap(service::run));
    }
}
