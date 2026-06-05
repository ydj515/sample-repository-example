package com.example.webfluxwithredisexample.application;

import com.example.webfluxwithredisexample.domain.SortedSetModel;
import com.example.webfluxwithredisexample.infrastructure.repository.SortedSetRepository;
import com.example.webfluxwithredisexample.presentation.router.sortedset.SortedSetConditionalAddRequest;
import com.example.webfluxwithredisexample.presentation.router.sortedset.SortedSetDeltaRequest;
import com.example.webfluxwithredisexample.presentation.router.sortedset.SortedSetRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SortedSetAsyncService {
    private final SortedSetRepository redis;

    public Mono<Void> setSortedSet(SortedSetRequest req) {
        return redis.addToSortedSet(req.baseRequest().key(), req.name(), req.score())
                .then();
    }

    public Mono<Set<SortedSetModel>> getSetDataByRange(String key, Double min, Double max) {
        return redis.rangeByScore(key, min, max)
                .collectList()
                .map(HashSet::new);
    }

    public Mono<List<SortedSetModel>> getTopN(String key, Integer n) {
        return redis.getTopNFromSortedSet(key, n)
                .collectList();
    }

    public Mono<Long> rank(String key, String member) {
        return redis.rank(key, member);
    }

    public Mono<Long> reverseRank(String key, String member) {
        return redis.reverseRank(key, member);
    }

    public Mono<Double> incrementScore(SortedSetDeltaRequest req) {
        return redis.incrementScore(req.baseRequest().key(), req.name(), req.delta());
    }

    public Mono<Double> score(String key, String member) {
        return redis.score(key, member);
    }

    public Mono<Long> addWithOption(SortedSetConditionalAddRequest req) {
        return redis.addToSortedSetWithOption(req.baseRequest().key(), req.name(), req.score(), req.option());
    }
}
