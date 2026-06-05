package com.example.webfluxwithredisexample.application;

import com.example.webfluxwithredisexample.infrastructure.repository.SetRepository;
import com.example.webfluxwithredisexample.presentation.router.set.SetMembersRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SetAsyncService {
    private final SetRepository redis;

    public Mono<Long> add(SetMembersRequest req) {
        return redis.add(req.baseRequest().key(), req.members());
    }

    public Mono<Long> remove(SetMembersRequest req) {
        return redis.remove(req.baseRequest().key(), req.members());
    }

    public Mono<List<String>> members(String key) {
        return redis.members(key).collectList();
    }

    public Mono<Boolean> isMember(String key, String member) {
        return redis.isMember(key, member);
    }

    public Mono<Long> size(String key) {
        return redis.size(key);
    }

    public Mono<List<String>> pop(String key, long count) {
        if (count <= 1) {
            return redis.pop(key)
                    .map(List::of)
                    .defaultIfEmpty(List.of());
        }

        return redis.pop(key, count).collectList();
    }

    public Mono<List<String>> randomMembers(String key, long count, boolean distinct) {
        if (count <= 1) {
            return redis.randomMember(key)
                    .map(List::of)
                    .defaultIfEmpty(List.of());
        }

        return redis.randomMembers(key, count, distinct).collectList();
    }

    public Mono<List<String>> intersect(List<String> keys) {
        return redis.intersect(keys).collectList();
    }

    public Mono<List<String>> union(List<String> keys) {
        return redis.union(keys).collectList();
    }

    public Mono<List<String>> difference(List<String> keys) {
        return redis.difference(keys).collectList();
    }
}
