package com.example.webfluxwithredisexample.infrastructure.repository.usecase.orderevent;

import com.example.webfluxwithredisexample.domain.usecase.orderevent.OrderEvent;
import com.example.webfluxwithredisexample.domain.usecase.orderevent.StreamFailureResult;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class OrderStreamRepository {
    @Value("${app.usecase.order-stream.key:usecase:orders:stream}")
    private String key;
    private static final String GROUP = "order-workers";
    private final ReactiveRedisTemplate<String, String> template;
    private final Gson gson;

    public Mono<String> publish(OrderEvent event) {
        return template.opsForStream()
                .add(StreamRecords.string(Map.of("event", gson.toJson(event))).withStreamKey(key))
                .map(RecordId::getValue);
    }

    public Mono<String> createGroup() {
        return template.opsForStream()
                .createGroup(key, ReadOffset.from("0-0"), GROUP)
                .onErrorResume(
                        error -> {
                            Throwable cause = error;
                            while (cause != null) {
                                if (cause.getMessage() != null
                                        && cause.getMessage().contains("BUSYGROUP")) {
                                    return Mono.just("EXISTS");
                                }
                                cause = cause.getCause();
                            }
                            return Mono.error(error);
                        });
    }

    public Flux<MapRecord<String, Object, Object>> read(String consumer, boolean pending) {
        return template.opsForStream()
                .read(
                        Consumer.from(GROUP, consumer),
                        StreamReadOptions.empty().count(20),
                        StreamOffset.create(
                                key, pending ? ReadOffset.from("0-0") : ReadOffset.lastConsumed()));
    }

    private static final RedisScript<String> PENDING =
            RedisScript.of(
                    new ClassPathResource("lua/usecase/orderevent/pending.lua"), String.class);

    public Mono<PendingMessages> pending(String cursor, int count) {
        String start = "0-0".equals(cursor) ? "-" : "(" + cursor;
        // Spring Data Redis 3.4.4의 Lettuce pending 범위 변환 문제를 우회한다.
        return template.execute(PENDING, List.of(key), GROUP, start, Integer.toString(count))
                .single()
                .map(
                        json -> {
                            var result = JsonParser.parseString(json);
                            List<PendingMessage> messages = new ArrayList<>();
                            if (result.isJsonArray()) {
                                for (var element : result.getAsJsonArray()) {
                                    var row = element.getAsJsonArray();
                                    messages.add(
                                            new PendingMessage(
                                                    RecordId.of(row.get(0).getAsString()),
                                                    Consumer.from(GROUP, row.get(1).getAsString()),
                                                    Duration.ofMillis(row.get(2).getAsLong()),
                                                    row.get(3).getAsLong()));
                                }
                            }
                            return new PendingMessages(GROUP, messages);
                        });
    }

    public Flux<MapRecord<String, Object, Object>> claim(
            String consumer, Duration minIdle, RecordId... ids) {
        if (ids.length == 0) {
            return Flux.empty();
        }
        // XPENDING 이후 소유자가 바뀌어도 Redis가 idle 조건을 다시 확인한다.
        return template.opsForStream().claim(key, GROUP, consumer, minIdle, ids);
    }

    public OrderEvent event(MapRecord<String, Object, Object> record) {
        return gson.fromJson(record.getValue().get("event").toString(), OrderEvent.class);
    }

    private static final RedisScript<List> RECORD_FAILURE =
            RedisScript.of(
                    new ClassPathResource("lua/usecase/orderevent/record-failure.lua"), List.class);

    private static final RedisScript<Long> ACK =
            RedisScript.of(
                    new ClassPathResource("lua/usecase/orderevent/ack-and-clear.lua"), Long.class);

    public Mono<Long> failureCount(String id) {
        return template.<String, String>opsForHash()
                .get(key + ":failures", id)
                .map(Long::parseLong)
                .defaultIfEmpty(0L);
    }

    public Mono<StreamFailureResult> recordFailure(
            String id, String consumer, int maxAttempts, String errorType) {
        return template.execute(
                        RECORD_FAILURE,
                        List.of(key, key + ":failures", key + ":dlq"),
                        GROUP,
                        id,
                        consumer,
                        Integer.toString(maxAttempts),
                        errorType)
                .single()
                .map(
                        values ->
                                new StreamFailureResult(
                                        values.get(0).toString(),
                                        Long.parseLong(values.get(1).toString()),
                                        values.get(2).toString(),
                                        Long.parseLong(values.get(3).toString())));
    }

    public Flux<Map<String, Object>> deadLetters(int count) {
        return template.opsForStream()
                .range(key + ":dlq", Range.unbounded(), Limit.limit().count(count))
                .map(
                        record ->
                                Map.<String, Object>of(
                                        "recordId",
                                        record.getId().getValue(),
                                        "fields",
                                        record.getValue()));
    }

    public Mono<Long> ack(String id) {
        return template.execute(ACK, List.of(key, key + ":failures"), GROUP, id).single();
    }
}
