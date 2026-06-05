package com.example.webfluxwithredisexample.infrastructure.repository;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.example.webfluxwithredisexample.domain.SortedSetModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveZSetCommands.ZAddCommand;
import org.springframework.data.redis.connection.zset.DefaultTuple;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Repository
public class SortedSetRepository {
    private final ReactiveRedisTemplate<String, String> template;
    private final Gson gson;
    private final ReactiveRedisConnectionFactory connectionFactory;

    @Value("${app.redis.default-ttl}")
    private Duration defaultExpireTime;

    public Mono<Boolean> addToSortedSet(String key, String member, Double score) {
        return template.opsForZSet().add(key, gson.toJson(member), score)
                .delayUntil(result -> template.expire(key, defaultExpireTime));
    }

    public Mono<Long> addToSortedSetWithOption(String key, String member, Double score, String option) {
        ZAddCommand command = applyOption(
                ZAddCommand.tuple(new DefaultTuple(gson.toJson(member).getBytes(StandardCharsets.UTF_8), score))
                        .to(serializeByteBuffer(key)),
                option
        );

        return Mono.usingWhen(
                Mono.fromSupplier(connectionFactory::getReactiveConnection),
                connection -> connection.zSetCommands()
                        .zAdd(Mono.just(command))
                        .next()
                        .map(response -> response.getOutput().longValue()),
                ReactiveRedisConnection::closeLater
        ).delayUntil(result -> template.expire(key, defaultExpireTime));
    }

    public Flux<SortedSetModel> rangeByScore(String key, double minScore, double maxScore) {
        return template.opsForZSet().rangeByScoreWithScores(key, Range.closed(minScore, maxScore))
                .map(tuple -> new SortedSetModel(deserializeString(tuple.getValue()), tuple.getScore()));
    }

    public Flux<SortedSetModel> getTopNFromSortedSet(String key, int n) {
        return template.opsForZSet().reverseRangeWithScores(key, Range.closed(0L, (long) n - 1))
                .map(tuple -> new SortedSetModel(deserializeString(tuple.getValue()), tuple.getScore()));
    }

    public Mono<Long> rank(String key, String member) {
        return template.opsForZSet().rank(key, gson.toJson(member));
    }

    public Mono<Long> reverseRank(String key, String member) {
        return template.opsForZSet().reverseRank(key, gson.toJson(member));
    }

    public Mono<Double> incrementScore(String key, String member, double delta) {
        return template.opsForZSet().incrementScore(key, gson.toJson(member), delta);
    }

    public Mono<Double> score(String key, String member) {
        return template.opsForZSet().score(key, gson.toJson(member));
    }

    private ZAddCommand applyOption(ZAddCommand command, String option) {
        if (option == null || option.isBlank()) {
            return command;
        }

        return switch (option.toUpperCase()) {
            case "NX" -> command.nx();
            case "XX" -> command.xx();
            case "GT" -> command.gt();
            case "LT" -> command.lt();
            case "CH" -> command.ch();
            default -> throw new IllegalArgumentException("Unsupported sorted set option: " + option);
        };
    }

    private ByteBuffer serializeByteBuffer(String value) {
        return ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
    }

    private String deserializeString(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        try {
            return gson.fromJson(rawValue, String.class);
        } catch (JsonSyntaxException e) {
            return rawValue;
        }
    }

}
