package com.example.webfluxwithredisexample.application.usecase.cart;

import com.example.webfluxwithredisexample.infrastructure.repository.usecase.cart.SessionRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionRepository sessions;

    public Mono<String> create(String user) {
        if (user == null || user.isBlank()) {
            return Mono.error(new IllegalArgumentException("userId is required"));
        }
        return sessions.create(user);
    }

    public Mono<String> touch(String id) {
        return sessions.touch(id)
                .switchIfEmpty(
                        Mono.error(
                                new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED, "Session expired")));
    }
}
