package com.example.webfluxwithredisexample.application.usecase.popularity;

import com.example.webfluxwithredisexample.infrastructure.repository.usecase.popularity.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.*;

@Service
@RequiredArgsConstructor
public class WishlistService {
    private final WishlistRepository wishlist;

    public Mono<Long> add(String user, String product) {
        return wishlist.add(user, product);
    }

    public Flux<String> get(String user) {
        return wishlist.members(user);
    }
}
