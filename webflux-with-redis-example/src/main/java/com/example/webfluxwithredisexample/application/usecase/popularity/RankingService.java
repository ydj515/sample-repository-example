package com.example.webfluxwithredisexample.application.usecase.popularity;

import com.example.webfluxwithredisexample.domain.usecase.popularity.RankingEntry;
import com.example.webfluxwithredisexample.infrastructure.repository.usecase.popularity.RankingRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.*;

@Service
@RequiredArgsConstructor
public class RankingService {
    private final RankingRepository ranking;

    public Mono<Double> increment(String product) {
        return ranking.increment(product);
    }

    public Flux<RankingEntry> top(int limit) {
        if (limit < 1 || limit > 100) {
            return Flux.error(new IllegalArgumentException("limit must be 1..100"));
        }
        return ranking.top(limit);
    }
}
