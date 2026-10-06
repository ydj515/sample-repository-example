package com.example.webfluxwithredisexample.presentation.router.usecase.popularity;

import com.example.webfluxwithredisexample.presentation.router.usecase.UsecaseResponses;

import org.springframework.context.annotation.*;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ServerWebInputException;

import reactor.core.publisher.Mono;

@Configuration
public class PopularityRouter {
    @Bean
    public RouterFunction<ServerResponse> popularityUsecaseRoute(PopularityHandler handler) {
        return RouterFunctions.route()
                .PUT("/usecases/popularity/wishlists/{userId}/{productId}", request -> Mono.defer(() -> handler.wish(request)))
                .GET("/usecases/popularity/wishlists/{userId}", request -> Mono.defer(() -> handler.wishes(request)))
                .POST("/usecases/popularity/ranking/{productId}", request -> Mono.defer(() -> handler.score(request)))
                .GET("/usecases/popularity/ranking", request -> Mono.defer(() -> handler.top(request)))
                .POST("/usecases/popularity/visits/{productId}/{visitorId}", request -> Mono.defer(() -> handler.visit(request)))
                .GET("/usecases/popularity/visits/{productId}/{visitorId}", request -> Mono.defer(() -> handler.visited(request)))
                .onError(IllegalArgumentException.class, UsecaseResponses::badRequest)
                .onError(ServerWebInputException.class, UsecaseResponses::badRequest)
                .build();
    }
}
