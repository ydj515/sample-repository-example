package com.example.webfluxwithredisexample.application;

import com.example.webfluxwithredisexample.domain.StrategyUser;
import com.example.webfluxwithredisexample.domain.StrategyWriteBehindTask;
import com.example.webfluxwithredisexample.domain.ValueWithTTL;
import com.example.webfluxwithredisexample.infrastructure.repository.StrategyRepository;
import com.example.webfluxwithredisexample.infrastructure.repository.StrategyUserFakeDbRepository;
import com.example.webfluxwithredisexample.presentation.router.strategy.StrategyQueueProcessResponse;
import com.example.webfluxwithredisexample.presentation.router.strategy.StrategyRefreshRequest;
import com.example.webfluxwithredisexample.presentation.router.strategy.StrategyUserRequest;
import com.example.webfluxwithredisexample.presentation.router.strategy.StrategyUserResponse;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisStrategyService {
    private static final String WRITE_BEHIND_QUEUE_KEY = "strategy:write_queue";
    private static final int LOCK_RETRY_LIMIT = 5;
    private static final Duration LOCK_RETRY_DELAY = Duration.ofMillis(100);

    private final StrategyRepository redis;
    private final StrategyUserFakeDbRepository fakeDb;
    private final RedissonReactiveClient redissonReactiveClient;
    private final Gson gson;

    public Mono<StrategyUser> getDbUser(Long userId) {
        return fakeDb.findById(userId);
    }

    public Mono<StrategyUser> saveDbUser(StrategyUserRequest req) {
        return fakeDb.save(toUser(req));
    }

    public Mono<StrategyUserResponse> getUserByCacheAside(Long userId) {
        return redis.getValueWithTtl(userCacheKey(userId), StrategyUser.class)
                .map(value -> response("cache-aside", "cache", value.getValue(), value.getTTL(), false, null))
                .switchIfEmpty(loadFromDbAndCache(userId, "cache-aside", "db"));
    }

    public Mono<StrategyUserResponse> updateUserByCacheAside(StrategyUserRequest req) {
        StrategyUser user = toUser(req);
        return fakeDb.save(user)
                .flatMap(saved -> redis.delete(userCacheKey(saved.getId()))
                        .thenReturn(response("cache-aside", "db+cache-evict", saved, null, false, "캐시 무효화 완료")));
    }

    public Mono<StrategyUserResponse> getUserByWriteThrough(Long userId) {
        return redis.getValueWithTtl(userCacheKey(userId), StrategyUser.class)
                .map(value -> response("write-through", "cache", value.getValue(), value.getTTL(), false, null))
                .switchIfEmpty(loadFromDbAndCache(userId, "write-through", "db"));
    }

    public Mono<StrategyUserResponse> updateUserByWriteThrough(StrategyUserRequest req) {
        StrategyUser user = toUser(req);
        return redis.setData(userCacheKey(user.getId()), user)
                .then(fakeDb.save(user))
                .map(saved -> response("write-through", "cache+db", saved, redis.getDefaultExpireTime(), false, "캐시와 DB를 함께 갱신했습니다."));
    }

    public Mono<StrategyUserResponse> getUserByReadThrough(Long userId) {
        return redis.getValueWithTtl(userCacheKey(userId), StrategyUser.class)
                .map(value -> response("read-through", "cache", value.getValue(), value.getTTL(), false, null))
                .switchIfEmpty(loadFromDbAndCache(userId, "read-through", "loader"));
    }

    public Mono<StrategyUserResponse> updateUserByWriteBehind(StrategyUserRequest req) {
        StrategyUser user = toUser(req);
        StrategyWriteBehindTask task = new StrategyWriteBehindTask("update_user", user.getId(), user);

        return redis.setData(userCacheKey(user.getId()), user)
                .then(redis.enqueue(WRITE_BEHIND_QUEUE_KEY, task))
                .map(queueSize -> new StrategyUserResponse(
                        "write-behind",
                        "cache+queue",
                        user,
                        toSeconds(redis.getDefaultExpireTime()),
                        null,
                        false,
                        true,
                        queueSize,
                        "DB 반영은 큐 처리 API에서 수행됩니다."
                ));
    }

    public Mono<StrategyQueueProcessResponse> processWriteBehindQueue() {
        return redis.dequeue(WRITE_BEHIND_QUEUE_KEY)
                .flatMap(payload -> {
                    StrategyWriteBehindTask task = gson.fromJson(payload, StrategyWriteBehindTask.class);
                    if (task == null || task.getData() == null) {
                        return redis.queueSize(WRITE_BEHIND_QUEUE_KEY)
                                .map(size -> new StrategyQueueProcessResponse(false, null, size, "처리할 작업이 올바르지 않습니다."));
                    }

                    return fakeDb.save(task.getData())
                            .zipWith(redis.queueSize(WRITE_BEHIND_QUEUE_KEY))
                            .map(tuple -> new StrategyQueueProcessResponse(true, tuple.getT1(), tuple.getT2(), "큐 작업을 DB에 반영했습니다."));
                })
                .switchIfEmpty(redis.queueSize(WRITE_BEHIND_QUEUE_KEY)
                        .map(size -> new StrategyQueueProcessResponse(false, null, size, "처리할 큐 작업이 없습니다.")));
    }

    public Mono<StrategyUserResponse> getUserByPer(Long userId) {
        return redis.getValueWithTtl(userCacheKey(userId), StrategyUser.class)
                .flatMap(value -> {
                    double probability = calculatePerProbability(value.getTTL());

                    if (Math.random() < probability) {
                        return fakeDb.findById(userId)
                                .flatMap(user -> redis.setData(userCacheKey(userId), user)
                                        .thenReturn(new StrategyUserResponse(
                                                "per",
                                                "db-recomputed",
                                                user,
                                                toSeconds(redis.getDefaultExpireTime()),
                                                probability,
                                                true,
                                                false,
                                                null,
                                                "TTL이 줄어든 시점에 조기 재계산을 수행했습니다."
                                        )));
                    }

                    return Mono.just(new StrategyUserResponse(
                            "per",
                            "cache",
                            value.getValue(),
                            toSeconds(value.getTTL()),
                            probability,
                            false,
                            false,
                            null,
                            "캐시를 그대로 사용했습니다."
                    ));
                })
                .switchIfEmpty(loadFromDbAndCache(userId, "per", "db-miss"));
    }

    public Mono<StrategyUserResponse> getUserByLock(Long userId) {
        return redis.getValueWithTtl(userCacheKey(userId), StrategyUser.class)
                .map(value -> response("lock", "cache", value.getValue(), value.getTTL(), false, null))
                .switchIfEmpty(loadWithLock(userId, LOCK_RETRY_LIMIT));
    }

    public Mono<List<StrategyUser>> refreshUsers(StrategyRefreshRequest req) {
        Flux<StrategyUser> targetUsers = (req.userIds() == null || req.userIds().isEmpty())
                ? fakeDb.findAll()
                : fakeDb.findAllByIds(req.userIds());

        return targetUsers
                .concatMap(user -> redis.setData(userCacheKey(user.getId()), user).thenReturn(user))
                .collectList();
    }

    public <T> Mono<ValueWithTTL<T>> getValueWithTtl(String key, Class<T> clazz) {
        return redis.getValueWithTtl(key, clazz);
    }

    private Mono<StrategyUserResponse> loadFromDbAndCache(Long userId, String strategy, String source) {
        return fakeDb.findById(userId)
                .flatMap(user -> redis.setData(userCacheKey(userId), user)
                        .thenReturn(response(strategy, source, user, redis.getDefaultExpireTime(), true, "DB 조회 후 캐시에 적재했습니다.")));
    }

    private Mono<StrategyUserResponse> loadWithLock(Long userId, int retriesLeft) {
        String lockKey = lockKey(userId);
        RLockReactive lock = redissonReactiveClient.getLock(lockKey);

        return lock.tryLock(0, 10, TimeUnit.SECONDS)
                .flatMap(acquired -> {
                    if (!acquired) {
                        if (retriesLeft <= 0) {
                            return Mono.error(new IllegalStateException("락 획득에 실패했습니다."));
                        }

                        return Mono.delay(LOCK_RETRY_DELAY)
                                .then(loadWithLock(userId, retriesLeft - 1));
                    }

                    return fakeDb.findById(userId)
                            .flatMap(user -> redis.setData(userCacheKey(userId), user)
                                    .thenReturn(response("lock", "db-locked", user, redis.getDefaultExpireTime(), true, "락을 획득한 요청이 DB를 조회했습니다.")))
                            .flatMap(result -> lock.unlock().thenReturn(result))
                            .onErrorResume(e -> lock.unlock().then(Mono.error(e)));
                });
    }

    private double calculatePerProbability(Duration remainTtl) {
        long defaultSeconds = Math.max(redis.getDefaultExpireTime().getSeconds(), 1L);
        long remainSeconds = Math.max(remainTtl.getSeconds(), 0L);
        double delta = (double) (defaultSeconds - remainSeconds) / defaultSeconds;
        double beta = 1.0d;
        return Math.max(0.0d, Math.min(1.0d, delta * beta));
    }

    private StrategyUserResponse response(
            String strategy,
            String source,
            StrategyUser user,
            Duration ttl,
            boolean refreshed,
            String message
    ) {
        return new StrategyUserResponse(
                strategy,
                source,
                user,
                toSeconds(ttl),
                null,
                refreshed,
                false,
                null,
                message
        );
    }

    private StrategyUser toUser(StrategyUserRequest req) {
        return new StrategyUser(req.userId(), req.name(), req.email(), req.age(), null);
    }

    private Long toSeconds(Duration ttl) {
        return ttl == null ? null : ttl.getSeconds();
    }

    private String userCacheKey(Long userId) {
        return "strategy:user:" + userId;
    }

    private String lockKey(Long userId) {
        return "lock:strategy:user:" + userId;
    }
}
