package com.example.webfluxwithredisexample.presentation.router.usecase.inventory;

import com.example.webfluxwithredisexample.presentation.router.usecase.UsecaseResponses;

import org.springframework.context.annotation.*;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ServerWebInputException;

import reactor.core.publisher.Mono;

@Configuration
public class InventoryRouter {
    @Bean
    public RouterFunction<ServerResponse> inventoryUsecaseRoute(InventoryHandler handler) {
        return RouterFunctions.route()
                .POST("/usecases/inventory/simulations", request -> Mono.defer(() -> handler.simulate(request)))
                .onError(IllegalArgumentException.class, UsecaseResponses::badRequest)
                .onError(ServerWebInputException.class, UsecaseResponses::badRequest)
                .build();
    }
}
