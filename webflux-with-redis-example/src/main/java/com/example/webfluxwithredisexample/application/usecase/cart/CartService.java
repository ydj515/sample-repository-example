package com.example.webfluxwithredisexample.application.usecase.cart;

import com.example.webfluxwithredisexample.domain.usecase.cart.CartItem;
import com.example.webfluxwithredisexample.infrastructure.repository.usecase.cart.CartRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cart;

    private final SessionService sessions;

    public Mono<List<CartItem>> get(String session) {
        return sessions.touch(session).then(Mono.defer(() -> cart.get(session)));
    }

    public Mono<Long> put(String session, CartItem item) {
        if (item.productId() == null || item.productId().isBlank() || item.quantity() < 0) {
            return Mono.error(
                    new IllegalArgumentException(
                            "Valid productId and nonnegative quantity required"));
        }
        return sessions.touch(session).then(Mono.defer(() -> cart.put(session, item)));
    }
}
