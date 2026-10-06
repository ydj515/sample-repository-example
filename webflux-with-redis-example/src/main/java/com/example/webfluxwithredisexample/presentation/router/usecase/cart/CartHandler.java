package com.example.webfluxwithredisexample.presentation.router.usecase.cart;

import static com.example.webfluxwithredisexample.presentation.router.usecase.UsecaseResponses.ok;

import com.example.webfluxwithredisexample.application.usecase.cart.*;
import com.example.webfluxwithredisexample.domain.usecase.cart.CartItem;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;

import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CartHandler {
    private final CartService cart;

    private final SessionService sessions;

    public record SessionRequest(String userId) {
    }

    public Mono<ServerResponse> session(ServerRequest req) {
        return ok(
                req.bodyToMono(SessionRequest.class)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Body required")))
                        .flatMap(body -> sessions.create(body.userId()))
                        .map(id -> Map.of("sessionId", id)));
    }

    public Mono<ServerResponse> get(ServerRequest req) {
        return ok(cart.get(req.pathVariable("sessionId")));
    }

    public Mono<ServerResponse> put(ServerRequest req) {
        return ok(
                req.bodyToMono(CartItem.class)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Body required")))
                        .flatMap(item -> cart.put(req.pathVariable("sessionId"), item))
                        .map(size -> Map.of("itemCount", size)));
    }
}
