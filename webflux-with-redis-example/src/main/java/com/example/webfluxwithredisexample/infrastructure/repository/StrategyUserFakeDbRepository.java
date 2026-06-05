package com.example.webfluxwithredisexample.infrastructure.repository;

import com.example.webfluxwithredisexample.domain.StrategyUser;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class StrategyUserFakeDbRepository {
    private final Map<Long, StrategyUser> storage = new ConcurrentHashMap<>();

    public Mono<StrategyUser> findById(Long userId) {
        return Mono.just(copy(storage.computeIfAbsent(userId, this::defaultUser)));
    }

    public Mono<StrategyUser> save(StrategyUser user) {
        StrategyUser toSave = copy(user);
        toSave.setUpdatedAt(Instant.now().toString());
        storage.put(toSave.getId(), toSave);
        return Mono.just(copy(toSave));
    }

    public Flux<StrategyUser> findAllByIds(List<Long> userIds) {
        return Flux.fromIterable(userIds)
                .flatMap(this::findById);
    }

    public Flux<StrategyUser> findAll() {
        return Flux.fromIterable(storage.values())
                .map(this::copy);
    }

    private StrategyUser defaultUser(Long userId) {
        return new StrategyUser(
                userId,
                "user-" + userId,
                "user" + userId + "@test.com",
                20 + Math.toIntExact(userId % 10),
                Instant.now().toString()
        );
    }

    private StrategyUser copy(StrategyUser user) {
        return new StrategyUser(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAge(),
                user.getUpdatedAt()
        );
    }
}
