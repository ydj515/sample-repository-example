package com.example.webfluxwithredisexample.presentation.router.usecase.orderevent;

import com.example.webfluxwithredisexample.presentation.router.usecase.UsecaseResponses;

import org.springframework.context.annotation.*;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ServerWebInputException;

import reactor.core.publisher.Mono;

@Configuration
public class OrderEventRouter {
    @Bean
    public RouterFunction<ServerResponse> ordereventUsecaseRoute(OrderEventHandler handler) {
        return RouterFunctions.route()
                .POST("/usecases/order-events/{transport}/publish", request -> Mono.defer(() -> handler.publish(request)))
                .POST("/usecases/order-events/list/process-next", request -> Mono.defer(() -> handler.next(request)))
                .GET("/usecases/order-events/pubsub/subscribe", request -> Mono.defer(() -> handler.listen(request)))
                .POST("/usecases/order-events/stream/group", request -> Mono.defer(() -> handler.group(request)))
                .POST("/usecases/order-events/stream/process", request -> Mono.defer(() -> handler.process(request)))
                .POST("/usecases/order-events/stream/reserve", request -> Mono.defer(() -> handler.reserve(request)))
                .GET("/usecases/order-events/stream/pending", request -> Mono.defer(() -> handler.pending(request)))
                .POST("/usecases/order-events/stream/recover", request -> Mono.defer(() -> handler.recover(request)))
                .GET("/usecases/order-events/stream/dlq", request -> Mono.defer(() -> handler.deadLetters(request)))
                .onError(IllegalArgumentException.class, UsecaseResponses::badRequest)
                .onError(ServerWebInputException.class, UsecaseResponses::badRequest)
                .build();
    }
}
