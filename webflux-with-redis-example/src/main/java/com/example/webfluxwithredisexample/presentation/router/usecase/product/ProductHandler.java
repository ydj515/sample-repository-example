package com.example.webfluxwithredisexample.presentation.router.usecase.product;

import static com.example.webfluxwithredisexample.presentation.router.usecase.UsecaseResponses.ok;

import com.example.webfluxwithredisexample.application.usecase.product.ProductCacheService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;

import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProductHandler {
    private final ProductCacheService service;

    public Mono<ServerResponse> get(ServerRequest req) {
        return ok(service.get(
                req.pathVariable("id"),
                Boolean.parseBoolean(req.queryParam("protected").orElse("true")))
        );
    }

    public Mono<ServerResponse> ttl(ServerRequest req) {
        return ok(service.ttl(req.pathVariable("id")).map(ttl -> Map.of("ttlSeconds", ttl)));
    }

    public Mono<ServerResponse> stats(ServerRequest req) {
        return ServerResponse.ok().bodyValue(Map.of("dbReads", service.dbReads()));
    }
}
