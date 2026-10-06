package com.example.webfluxwithredisexample.infrastructure.repository.usecase.popularity;

import com.example.webfluxwithredisexample.domain.usecase.popularity.RankingEntry;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class RankingRepository {
    private final ReactiveRedisTemplate<String, String> template;

    public Mono<Double> increment(String product) {
        return template.opsForZSet().incrementScore("usecase:ranking", product, 1);
    }

    public Flux<RankingEntry> top(int limit) {
        return template.opsForZSet()
                .reverseRangeWithScores("usecase:ranking", Range.closed(0L, (long) limit - 1))
                .map(tuple -> new RankingEntry(tuple.getValue(), tuple.getScore()));
    }
}
