package com.example.webfluxwithredisexample.presentation.router.usecase.product;

import com.example.webfluxwithredisexample.presentation.router.usecase.UsecaseResponses;

import org.springframework.context.annotation.*;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ServerWebInputException;

import reactor.core.publisher.Mono;

@Configuration
public class ProductRouter {
    @Bean
    public RouterFunction<ServerResponse> productUsecaseRoute(ProductHandler handler) {
        return RouterFunctions.route()
                .GET("/usecases/products/{id}", request -> Mono.defer(() -> handler.get(request)))
                .GET("/usecases/products/{id}/ttl", request -> Mono.defer(() -> handler.ttl(request)))
                .GET("/usecases/products/stats/db", request -> Mono.defer(() -> handler.stats(request)))
                .onError(IllegalArgumentException.class, UsecaseResponses::badRequest)
                .onError(ServerWebInputException.class, UsecaseResponses::badRequest)
                .build();
    }
}
