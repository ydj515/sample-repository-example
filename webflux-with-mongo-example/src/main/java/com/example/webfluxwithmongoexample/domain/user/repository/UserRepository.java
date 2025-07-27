package com.example.webfluxwithmongoexample.domain.user.repository;

import com.example.webfluxwithmongoexample.domain.user.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository {
    Mono<User> save(User user);

    Mono<User> findById(String id);

    Flux<User> findAll();
}
