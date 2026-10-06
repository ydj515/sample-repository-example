package com.example.webfluxwithredisexample.infrastructure.consumer.usecase.orderevent;

import com.example.webfluxwithredisexample.application.usecase.orderevent.OrderEventProcessor;
import com.example.webfluxwithredisexample.domain.usecase.orderevent.*;
import com.example.webfluxwithredisexample.infrastructure.repository.usecase.orderevent.OrderStreamRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.stereotype.Component;

import reactor.core.publisher.*;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OrderStreamConsumer {
    private final OrderStreamRepository repository;
    private final OrderEventProcessor processor;
    private int maxAttempts = 3;

    @Value("${app.usecase.order-stream.max-attempts:3}")
    public void setMaxAttempts(int maxAttempts) {
        if (maxAttempts < 1 || maxAttempts > 10) {
            throw new IllegalArgumentException("maxAttempts must be 1..10");
        }
        this.maxAttempts = maxAttempts;
    }

    public Mono<String> createGroup() {
        return repository.createGroup();
    }

    public Flux<Map<String, Object>> process(String consumer, boolean pending) {
        return process(consumer, pending, false);
    }

    public Flux<Map<String, Object>> process(
            String consumer, boolean pending, boolean failProcessing) {
        validateConsumer(consumer);
        return repository
                .read(consumer, pending)
                .concatMap(record -> processAndAck(record, consumer, failProcessing));
    }

    // 중단 상태 재현용: 전달만 받고 처리/ACK는 수행하지 않는다.
    public Flux<Map<String, Object>> reserve(String consumer) {
        validateConsumer(consumer);
        return repository
                .read(consumer, false)
                .map(
                        record ->
                                Map.<String, Object>of(
                                        "recordId",
                                        record.getId().getValue(),
                                        "event",
                                        repository.event(record),
                                        "acknowledged",
                                        0L));
    }

    public Mono<StreamPendingPage> pending(String cursor, int count) {
        validatePage(cursor, count);
        return repository
                .pending(cursor, count)
                .map(
                        messages ->
                                new StreamPendingPage(
                                        nextCursor(messages, count),
                                        messages.stream()
                                                .map(
                                                        message ->
                                                                new StreamPendingEntry(
                                                                        message.getIdAsString(),
                                                                        message.getConsumerName(),
                                                                        message.getElapsedTimeSinceLastDelivery()
                                                                                .toMillis(),
                                                                        message
                                                                                .getTotalDeliveryCount()))
                                                .toList()));
    }

    public Mono<StreamRecoveryResult> recover(
            String consumer, long minIdleMillis, String cursor, int count) {
        return recover(consumer, minIdleMillis, cursor, count, false);
    }

    public Mono<StreamRecoveryResult> recover(
            String consumer, long minIdleMillis, String cursor, int count, boolean failProcessing) {
        validateConsumer(consumer);
        validatePage(cursor, count);
        if (minIdleMillis < 1 || minIdleMillis > 86400000) {
            throw new IllegalArgumentException("minIdleMillis must be 1..86400000");
        }
        return repository
                .pending(cursor, count)
                .flatMap(
                        messages -> {
                            RecordId[] ids =
                                    messages.stream()
                                            .filter(
                                                    message ->
                                                            message.getElapsedTimeSinceLastDelivery()
                                                                            .toMillis()
                                                                    >= minIdleMillis)
                                            .map(PendingMessage::getId)
                                            .toArray(RecordId[]::new);
                            return repository
                                    .claim(consumer, Duration.ofMillis(minIdleMillis), ids)
                                    .concatMap(
                                            record ->
                                                    processAndAck(record, consumer, failProcessing))
                                    .collectList()
                                    .map(
                                            processed ->
                                                    new StreamRecoveryResult(
                                                            consumer,
                                                            nextCursor(messages, count),
                                                            messages.size(),
                                                            processed));
                        });
    }

    public Flux<Map<String, Object>> deadLetters(int count) {
        if (count < 1 || count > 100) {
            throw new IllegalArgumentException("count must be 1..100");
        }
        return repository.deadLetters(count);
    }

    private Mono<Map<String, Object>> processAndAck(
            MapRecord<String, Object, Object> record, String consumer, boolean failProcessing) {
        String id = record.getId().getValue();
        return repository
                .failureCount(id)
                .flatMap(
                        attempts -> {
                            if (attempts >= maxAttempts) {
                                return recordFailure(id, consumer, "RetryLimitExceeded");
                            }
                            return attemptProcessing(record, consumer, failProcessing);
                        });
    }

    private Mono<Map<String, Object>> attemptProcessing(
            MapRecord<String, Object, Object> record, String consumer, boolean failProcessing) {
        String id = record.getId().getValue();
        Mono<OrderEvent> processing =
                Mono.defer(
                        () ->
                                failProcessing
                                        ? Mono.error(
                                                new IllegalStateException(
                                                        "Simulated processing failure"))
                                        : processor.process(repository.event(record)));
        // ACK/Redis 인프라 오류는 처리 실패 횟수에 포함하지 않는다.
        return processing
                .materialize()
                .flatMap(
                        signal -> {
                            if (signal.hasValue()) {
                                return repository
                                        .ack(id)
                                        .map(
                                                ack ->
                                                        Map.<String, Object>of(
                                                                "recordId",
                                                                id,
                                                                "event",
                                                                signal.get(),
                                                                "status",
                                                                "PROCESSED",
                                                                "acknowledged",
                                                                ack));
                            }
                            String errorType =
                                    signal.hasError()
                                            ? signal.getThrowable().getClass().getSimpleName()
                                            : "EmptyProcessingResult";
                            return recordFailure(id, consumer, errorType);
                        });
    }

    private Mono<Map<String, Object>> recordFailure(String id, String consumer, String errorType) {
        return repository
                .recordFailure(id, consumer, maxAttempts, errorType)
                .map(
                        result ->
                                Map.<String, Object>of(
                                        "recordId",
                                        id,
                                        "status",
                                        result.status(),
                                        "attempts",
                                        result.attempts(),
                                        "dlqRecordId",
                                        result.dlqRecordId(),
                                        "acknowledged",
                                        result.acknowledged(),
                                        "errorType",
                                        errorType));
    }

    private String nextCursor(PendingMessages messages, int count) {
        return messages.size() == count ? messages.get(messages.size() - 1).getIdAsString() : "0-0";
    }

    private void validateConsumer(String consumer) {
        if (consumer == null || !consumer.matches("[a-zA-Z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Invalid consumer name");
        }
    }

    private void validatePage(String cursor, int count) {
        if (cursor == null
                || !cursor.matches("[0-9]{1,19}-[0-9]{1,19}")
                || count < 1
                || count > 100)
            throw new IllegalArgumentException("Invalid cursor or count (1..100)");
    }
}
