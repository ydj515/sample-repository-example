package com.example.webfluxwithredisexample.usecase;

import static org.mockito.Mockito.*;

import com.example.webfluxwithredisexample.application.usecase.cart.*;
import com.example.webfluxwithredisexample.application.usecase.inventory.*;
import com.example.webfluxwithredisexample.application.usecase.orderevent.*;
import com.example.webfluxwithredisexample.application.usecase.popularity.ProductVisitService;
import com.example.webfluxwithredisexample.domain.usecase.cart.CartItem;
import com.example.webfluxwithredisexample.domain.usecase.orderevent.OrderEvent;
import com.example.webfluxwithredisexample.domain.usecase.orderevent.StreamFailureResult;
import com.example.webfluxwithredisexample.infrastructure.consumer.usecase.orderevent.OrderStreamConsumer;
import com.example.webfluxwithredisexample.infrastructure.repository.usecase.cart.*;
import com.example.webfluxwithredisexample.infrastructure.repository.usecase.inventory.StockRepository;
import com.example.webfluxwithredisexample.infrastructure.repository.usecase.orderevent.*;
import com.example.webfluxwithredisexample.infrastructure.repository.usecase.popularity.ProductVisitRepository;
import com.example.webfluxwithredisexample.presentation.router.usecase.inventory.StockSimulationRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.*;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;

class UsecaseValidationTest {
    @Test
    void invalidCartDoesNotWriteRedis() {
        var repository = mock(CartRepository.class);
        var sessions = mock(SessionService.class);
        var service = new CartService(repository, sessions);
        StepVerifier.create(service.put("session", new CartItem("1", -1)))
                .expectError(IllegalArgumentException.class)
                .verify();
        verifyNoInteractions(repository, sessions);
    }

    @Test
    void expiredSessionStopsCartWrite() {
        var repository = mock(CartRepository.class);
        var sessions = mock(SessionRepository.class);
        when(sessions.touch("expired")).thenReturn(Mono.empty());
        // cart.put()에서 반환한 publisher는 세션 검증 이후에만 구독해야 한다.
        var cart = new CartService(repository, new SessionService(sessions));
        StepVerifier.create(cart.put("expired", new CartItem("1", 1)))
                .expectError(ResponseStatusException.class)
                .verify();
        verifyNoInteractions(repository);
    }

    @Test
    void invalidSimulationDoesNotInitializeStock() {
        var repository = mock(StockRepository.class);
        var inventory = mock(InventoryService.class);
        var service = new StockSimulationService(repository, inventory);
        StepVerifier.create(service.run(new StockSimulationRequest("unknown", 10, 20, 5, 1)))
                .expectError(IllegalArgumentException.class)
                .verify();
        verifyNoInteractions(repository, inventory);
    }

    @Test
    void bitmapOffsetIsBoundedBeforeRedisAllocation() {
        var repository = mock(ProductVisitRepository.class);
        var service = new ProductVisitService(repository);
        StepVerifier.create(service.record("1", Long.MAX_VALUE))
                .expectError(IllegalArgumentException.class)
                .verify();
        verifyNoInteractions(repository);
    }

    @Test
    void invalidOrderIsNotPublished() {
        var queue = mock(OrderQueueRepository.class);
        var pubsub = mock(OrderPubSubRepository.class);
        var stream = mock(OrderStreamRepository.class);
        var service = new OrderEventService(queue, pubsub, stream);
        StepVerifier.create(service.publish("stream", new OrderEvent("order", "1", 0)))
                .expectError(IllegalArgumentException.class)
                .verify();
        verifyNoInteractions(queue, pubsub, stream);
    }

    @Test
    void failedStreamProcessingLeavesMessagePending() {
        var repository = mock(OrderStreamRepository.class);
        var processor = mock(OrderEventProcessor.class);
        var record =
                StreamRecords.newRecord()
                        .in("usecase:orders:stream")
                        .ofMap(Map.<Object, Object>of("event", "payload"))
                        .withId(RecordId.of("1-0"));
        var event = new OrderEvent("order", "1", 1);
        when(repository.read("worker", false)).thenReturn(Flux.just(record));
        when(repository.event(record)).thenReturn(event);
        when(processor.process(event))
                .thenReturn(Mono.error(new IllegalStateException("Processing failed")));
        when(repository.failureCount("1-0")).thenReturn(Mono.just(0L));
        when(repository.recordFailure("1-0", "worker", 3, "IllegalStateException"))
                .thenReturn(Mono.just(new StreamFailureResult("RETRY_PENDING", 1, "", 0)));
        var consumer = new OrderStreamConsumer(repository, processor);
        StepVerifier.create(consumer.process("worker", false))
                .assertNext(row -> Assertions.assertEquals("RETRY_PENDING", row.get("status")))
                .verifyComplete();
        verify(repository, never()).ack(anyString());
    }

    @Test
    void claimedProcessingFailureDoesNotAcknowledge() {
        var repository = mock(OrderStreamRepository.class);
        var processor = mock(OrderEventProcessor.class);
        var id = RecordId.of("2-0");
        var pending =
                new PendingMessages(
                        "order-workers",
                        List.of(
                                new PendingMessage(
                                        id,
                                        Consumer.from("order-workers", "stopped"),
                                        Duration.ofMinutes(2),
                                        1)));
        var record =
                StreamRecords.newRecord()
                        .in("usecase:orders:stream")
                        .ofMap(Map.<Object, Object>of("event", "payload"))
                        .withId(id);
        var event = new OrderEvent("order", "1", 1);
        when(repository.pending("0-0", 20)).thenReturn(Mono.just(pending));
        when(repository.claim("recovery", Duration.ofMinutes(1), id)).thenReturn(Flux.just(record));
        when(repository.event(record)).thenReturn(event);
        when(processor.process(event))
                .thenReturn(Mono.error(new IllegalStateException("Processing failed")));
        when(repository.failureCount("2-0")).thenReturn(Mono.just(0L));
        when(repository.recordFailure("2-0", "recovery", 3, "IllegalStateException"))
                .thenReturn(Mono.just(new StreamFailureResult("RETRY_PENDING", 1, "", 0)));
        var consumer = new OrderStreamConsumer(repository, processor);
        StepVerifier.create(consumer.recover("recovery", 60000, "0-0", 20))
                .assertNext(
                        result ->
                                Assertions.assertEquals(
                                        "RETRY_PENDING",
                                        result.processed().getFirst().get("status")))
                .verifyComplete();
        verify(repository, never()).ack(anyString());
    }

    @Test
    void recoveryUsesRedisClaimResultAfterIdleRace() {
        var repository = mock(OrderStreamRepository.class);
        var processor = mock(OrderEventProcessor.class);
        var id = RecordId.of("3-0");
        var pending =
                new PendingMessages(
                        "order-workers",
                        List.of(
                                new PendingMessage(
                                        id,
                                        Consumer.from("order-workers", "stopped"),
                                        Duration.ofMinutes(2),
                                        1)));
        when(repository.pending("0-0", 20)).thenReturn(Mono.just(pending));
        // 후보 조회 후 다른 소비자가 회수했다면 XCLAIM 결과가 비어 있을 수 있다.
        when(repository.claim("recovery", Duration.ofMinutes(1), id)).thenReturn(Flux.empty());
        StepVerifier.create(
                        new OrderStreamConsumer(repository, processor)
                                .recover("recovery", 60000, "0-0", 20))
                .assertNext(result -> Assertions.assertTrue(result.processed().isEmpty()))
                .verifyComplete();
        verifyNoInteractions(processor);
        verify(repository, never()).ack(anyString());
    }

    @Test
    void ackInfrastructureFailureIsNotCountedAsProcessingFailure() {
        var repository = mock(OrderStreamRepository.class);
        var processor = mock(OrderEventProcessor.class);
        var record =
                StreamRecords.newRecord()
                        .in("stream")
                        .ofMap(Map.<Object, Object>of("event", "payload"))
                        .withId(RecordId.of("4-0"));
        var event = new OrderEvent("order", "1", 1);
        when(repository.read("worker", false)).thenReturn(Flux.just(record));
        when(repository.event(record)).thenReturn(event);
        when(processor.process(event)).thenReturn(Mono.just(event));
        when(repository.ack("4-0"))
                .thenReturn(Mono.error(new IllegalStateException("Redis unavailable")));
        when(repository.failureCount("4-0")).thenReturn(Mono.just(0L));
        StepVerifier.create(new OrderStreamConsumer(repository, processor).process("worker", false))
                .expectError(IllegalStateException.class)
                .verify();
        verify(repository, never()).recordFailure(anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    void retryLimitIsValidated() {
        var consumer =
                new OrderStreamConsumer(
                        mock(OrderStreamRepository.class), mock(OrderEventProcessor.class));
        Assertions.assertThrows(IllegalArgumentException.class, () -> consumer.setMaxAttempts(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> consumer.setMaxAttempts(11));
    }

    @Test
    void exhaustedRetryBudgetDoesNotInvokeProcessorAgain() {
        var repository = mock(OrderStreamRepository.class);
        var processor = mock(OrderEventProcessor.class);
        var record =
                StreamRecords.newRecord()
                        .in("stream")
                        .ofMap(Map.<Object, Object>of("event", "payload"))
                        .withId(RecordId.of("5-0"));
        when(repository.read("worker", true)).thenReturn(Flux.just(record));
        when(repository.failureCount("5-0")).thenReturn(Mono.just(3L));
        when(repository.recordFailure("5-0", "worker", 3, "RetryLimitExceeded"))
                .thenReturn(Mono.just(new StreamFailureResult("DEAD_LETTERED", 3, "6-0", 1)));
        StepVerifier.create(new OrderStreamConsumer(repository, processor).process("worker", true))
                .assertNext(row -> Assertions.assertEquals("DEAD_LETTERED", row.get("status")))
                .verifyComplete();
        verifyNoInteractions(processor);
    }
}
