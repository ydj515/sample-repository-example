package com.example.webfluxwithredisexample.infrastructure.repository;

import com.example.webfluxwithredisexample.presentation.router.stream.StreamEntryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Repository
public class StreamRepository {
    private final ReactiveRedisTemplate<String, String> template;

    public Mono<String> add(String key, Map<String, String> fields) {
        return template.opsForStream()
                .add(StreamRecords.string(fields).withStreamKey(key))
                .map(recordId -> recordId.getValue());
    }

    public Mono<Long> size(String key) {
        return template.opsForStream().size(key);
    }

    public Flux<StreamEntryResponse> read(String key, String offset, long count) {
        return template.opsForStream()
                .read(
                        StreamReadOptions.empty().count(count),
                        StreamOffset.create(key, ReadOffset.from(offset))
                )
                .map(this::toResponse);
    }

    public Mono<String> createGroup(String key, String offset, String group) {
        return template.opsForStream()
                .createGroup(key, ReadOffset.from(offset), group)
                .onErrorResume(e -> {
                    if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                        return Mono.just("BUSYGROUP");
                    }
                    return Mono.error(e);
                });
    }

    public Flux<StreamEntryResponse> readGroup(String key, String group, String consumer, long count) {
        return template.opsForStream()
                .read(
                        Consumer.from(group, consumer),
                        StreamReadOptions.empty().count(count),
                        StreamOffset.create(key, ReadOffset.lastConsumed())
                )
                .map(this::toResponse);
    }

    public Mono<Long> acknowledge(String key, String group, String recordId) {
        return template.opsForStream().acknowledge(key, group, recordId);
    }

    private StreamEntryResponse toResponse(MapRecord<String, Object, Object> record) {
        Map<String, String> fields = record.getValue().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        entry -> entry.getKey().toString(),
                        entry -> entry.getValue() == null ? null : entry.getValue().toString(),
                        (left, right) -> right,
                        java.util.LinkedHashMap::new
                ));

        return new StreamEntryResponse(record.getId().getValue(), fields);
    }
}
