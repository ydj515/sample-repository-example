package com.example.webfluxwithredisexample.presentation.router.usecase.cart;

import com.example.webfluxwithredisexample.presentation.router.usecase.UsecaseResponses;

import org.springframework.context.annotation.*;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ServerWebInputException;

import reactor.core.publisher.Mono;

@Configuration
public class CartRouter {
    @Bean
    public RouterFunction<ServerResponse> cartUsecaseRoute(CartHandler handler) {
        return RouterFunctions.route()
                .POST("/usecases/sessions", request -> Mono.defer(() -> handler.session(request)))
                .GET("/usecases/carts/{sessionId}", request -> Mono.defer(() -> handler.get(request)))
                .PUT("/usecases/carts/{sessionId}/items", request -> Mono.defer(() -> handler.put(request)))
                .onError(IllegalArgumentException.class, UsecaseResponses::badRequest)
                .onError(ServerWebInputException.class, UsecaseResponses::badRequest)
                .build();
    }
}
