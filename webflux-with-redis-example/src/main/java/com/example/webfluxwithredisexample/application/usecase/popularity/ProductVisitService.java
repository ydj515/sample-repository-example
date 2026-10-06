package com.example.webfluxwithredisexample.application.usecase.popularity;

import com.example.webfluxwithredisexample.infrastructure.repository.usecase.popularity.ProductVisitRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ProductVisitService {
    private final ProductVisitRepository visits;

    private void validate(long visitor) {
        if (visitor < 0 || visitor > 1000000)
            throw new IllegalArgumentException("visitor must be 0..1000000");
    }

    public Mono<Long> record(String product, long visitor) {
        return Mono.defer(
                () -> {
                    validate(visitor);
                    return visits.record(product, visitor);
                });
    }

    public Mono<Boolean> visited(String product, long visitor) {
        return Mono.defer(
                () -> {
                    validate(visitor);
                    return visits.visited(product, visitor);
                });
    }
}
