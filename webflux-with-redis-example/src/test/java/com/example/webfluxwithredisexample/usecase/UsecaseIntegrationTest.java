package com.example.webfluxwithredisexample.usecase;

import static org.junit.jupiter.api.Assertions.*;

import com.example.webfluxwithredisexample.application.usecase.cart.*;
import com.example.webfluxwithredisexample.application.usecase.inventory.StockSimulationService;
import com.example.webfluxwithredisexample.application.usecase.orderevent.OrderEventProcessor;
import com.example.webfluxwithredisexample.application.usecase.orderevent.OrderEventService;
import com.example.webfluxwithredisexample.application.usecase.popularity.*;
import com.example.webfluxwithredisexample.application.usecase.product.ProductCacheService;
import com.example.webfluxwithredisexample.domain.usecase.cart.CartItem;
import com.example.webfluxwithredisexample.domain.usecase.orderevent.OrderEvent;
import com.example.webfluxwithredisexample.infrastructure.consumer.usecase.orderevent.*;
import com.example.webfluxwithredisexample.infrastructure.repository.usecase.orderevent.OrderStreamRepository;
import com.example.webfluxwithredisexample.presentation.router.usecase.inventory.StockSimulationRequest;
import com.google.gson.Gson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(properties = {"spring.data.redis.password=", "spring.data.redis.database=15"})
@AutoConfigureWebTestClient
@DisabledIfEnvironmentVariable(named = "RUN_REDIS_USECASE_TESTS", matches = "(?i)false")
class UsecaseIntegrationTest {
    private static final String STREAM_KEY = "usecase:orders:test:" + UUID.randomUUID();

    @DynamicPropertySource
    static void streamKey(DynamicPropertyRegistry registry) {
        registry.add("app.usecase.order-stream.key", () -> STREAM_KEY);
    }

    @Autowired
    ProductCacheService products;

    @Autowired
    CartService carts;

    @Autowired
    SessionService sessions;

    @Autowired
    WishlistService wishes;

    @Autowired
    RankingService rankings;

    @Autowired
    ProductVisitService visits;

    @Autowired
    StockSimulationService simulations;

    @Autowired
    OrderEventService events;

    @Autowired
    OrderQueueConsumer queue;

    @Autowired
    OrderStreamConsumer stream;

    @Autowired
    OrderStreamRepository streamRepository;

    @Autowired
    ReactiveStringRedisTemplate redis;

    @Autowired
    WebTestClient client;

    @Test
    void protectedCacheLoadsOnceAndHasTtl() {
        // 키를 삭제하지 않고 만료시켜 cache miss를 준비한다.
        redis.expire("usecase:product:1", Duration.ZERO).block();
        long before = products.dbReads();
        var loaded =
                Flux.range(0, 12).flatMap(i -> products.get("1", true), 12).collectList().block();
        assertEquals(12, loaded.size());
        assertEquals(1, products.dbReads() - before);
        assertTrue(products.ttl("1").block() > 0);
        StepVerifier.create(products.get("missing", true))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void cartAndSessionRenewAndRejectExpiredSession() {
        String session = sessions.create("user-" + UUID.randomUUID()).block();
        carts.put(session, new CartItem("1", 2)).block();
        redis.expire("usecase:session:" + session, Duration.ofSeconds(5)).block();
        redis.expire("usecase:cart:" + session, Duration.ofSeconds(5)).block();
        assertEquals(2, carts.get(session).block().getFirst().quantity());
        assertTrue(redis.getExpire("usecase:session:" + session).block().getSeconds() > 1700);
        assertTrue(redis.getExpire("usecase:cart:" + session).block().getSeconds() > 1700);
        carts.put(session, new CartItem("1", 0)).block();
        assertTrue(carts.get(session).block().isEmpty());
        StepVerifier.create(carts.get("missing-session"))
                .expectError(ResponseStatusException.class)
                .verify();
    }

    @Test
    void setDeduplicatesAndVisitsTrackUniqueUsers() {
        String id = UUID.randomUUID().toString();
        assertEquals(1, wishes.add(id, "1").block());
        assertEquals(0, wishes.add(id, "1").block());
        assertEquals(List.of("1"), wishes.get(id).collectList().block());
        assertEquals(1.0, rankings.increment(id).block());
        assertEquals(2.0, rankings.increment(id).block());
        assertTrue(
                rankings.top(100).collectList().block().stream()
                        .anyMatch(entry -> entry.productId().equals(id) && entry.score() == 2));
        assertEquals(1, visits.record(id, 42).block());
        assertEquals(1, visits.record(id, 42).block());
        assertEquals(2, visits.record(id, 43).block());
        assertTrue(visits.visited(id, 42).block());
        assertFalse(visits.visited(id, 44).block());
    }

    @Test
    void stockModesExposeDifferentGuarantees() {
        var unsafe = simulations.run(new StockSimulationRequest("unsafe", 10, 50, 50, 1)).block();
        assertFalse(unsafe.consistent());
        var atomic = simulations.run(new StockSimulationRequest("atomic", 10, 50, 20, 1)).block();
        assertEquals(-40, atomic.remaining());
        assertFalse(atomic.consistent());
        for (String mode : List.of("lua", "lock")) {
            var result = simulations.run(new StockSimulationRequest(mode, 10, 50, 20, 1)).block();
            assertEquals(10, result.accepted());
            assertEquals(0, result.remaining());
            assertTrue(result.consistent());
        }
    }

    @Test
    void queueProcessesAndStreamAcknowledges() {
        OrderEvent event = new OrderEvent(UUID.randomUUID().toString(), "1", 2);
        events.publish("list", event).block();
        assertEquals(event, queue.next().block());
        assertNull(queue.next().block());
        events.publish("stream", event).block();
        stream.createGroup().block();
        String consumer = "test-" + UUID.randomUUID();
        var processed = stream.process(consumer, false).collectList().block();
        assertTrue(
                processed.stream()
                        .anyMatch(
                                row ->
                                        row.get("event").equals(event)
                                                && row.get("acknowledged").equals(1L)));
        assertTrue(stream.process(consumer, true).collectList().block().isEmpty());
        assertEquals(0L, events.publish("pubsub", event).block());
    }

    @Test
    void invalidHttpInputsAreRejected() {
        client.get()
                .uri("/usecases/popularity/ranking?limit=oops")
                .exchange()
                .expectStatus()
                .isBadRequest();
        client.post()
                .uri("/usecases/inventory/simulations")
                .bodyValue(new StockSimulationRequest("lua", 10, 1001, 20, 1))
                .exchange()
                .expectStatus()
                .isBadRequest();
        client.get().uri("/usecases/carts/expired").exchange().expectStatus().isUnauthorized();
        client.get().uri("/usecases/products/unknown").exchange().expectStatus().isNotFound();
    }

    @Test
    void idleMessagesTransferToRecoveryConsumerAndAreAcknowledged() {
        OrderEvent event = new OrderEvent(UUID.randomUUID().toString(), "1", 1);
        events.publish("stream", event).block();
        stream.createGroup().block();
        String stopped = "stopped-" + UUID.randomUUID();
        String recovery = "recovery-" + UUID.randomUUID();
        var reserved = stream.reserve(stopped).collectList().block();
        String id =
                reserved.stream()
                        .filter(row -> row.get("event").equals(event))
                        .findFirst()
                        .orElseThrow()
                        .get("recordId")
                        .toString();
        var before = stream.pending("0-0", 100).block();
        assertTrue(
                before.entries().stream()
                        .anyMatch(
                                entry ->
                                        entry.recordId().equals(id)
                                                && entry.consumer().equals(stopped)));
        // 새 메시지는 idle 기준을 충족하지 않아 회수하지 않는다.
        assertTrue(stream.recover(recovery, 86400000, "0-0", 100).block().processed().isEmpty());
        Mono.delay(Duration.ofMillis(10)).block();
        var result = stream.recover(recovery, 1, "0-0", 100).block();
        assertTrue(
                result.processed().stream()
                        .anyMatch(
                                row ->
                                        row.get("recordId").equals(id)
                                                && row.get("acknowledged").equals(1L)));
        assertTrue(stream.process(stopped, true).collectList().block().isEmpty());
        assertTrue(
                stream.pending("0-0", 100).block().entries().stream()
                        .noneMatch(entry -> entry.recordId().equals(id)));
    }

    @Test
    void recoveryCursorExcludesPreviousPageAndFinishesScan() {
        for (int i = 0; i < 3; i++) {
            events.publish("stream", new OrderEvent(UUID.randomUUID().toString(), "1", 1)).block();
        }
        stream.createGroup().block();
        stream.reserve("cursor-stopped").collectList().block();
        var page = stream.pending("0-0", 1).block();
        assertEquals(1, page.entries().size());
        assertNotEquals("0-0", page.nextCursor());
        var next = stream.pending(page.nextCursor(), 1).block();
        assertNotEquals(page.entries().getFirst().recordId(), next.entries().getFirst().recordId());
        Mono.delay(Duration.ofMillis(10)).block();
        String cursor = "0-0";
        int recovered = 0;
        int pages = 0;
        do {
            var result = stream.recover("cursor-recovery", 1, cursor, 1).block();
            recovered += result.processed().size();
            cursor = result.nextCursor();
            assertTrue(++pages <= 10);
        } while (!"0-0".equals(cursor));
        assertEquals(3, recovered);
        assertTrue(stream.pending("0-0", 100).block().entries().isEmpty());
    }

    @Test
    void invalidRecoveryHttpInputsAreRejected() {
        client.post()
                .uri("/usecases/order-events/stream/recover?minIdleMillis=0")
                .exchange()
                .expectStatus()
                .isBadRequest();
        client.post()
                .uri("/usecases/order-events/stream/recover?count=101")
                .exchange()
                .expectStatus()
                .isBadRequest();
        client.get()
                .uri("/usecases/order-events/stream/pending?cursor=invalid")
                .exchange()
                .expectStatus()
                .isBadRequest();
        client.post()
                .uri("/usecases/order-events/stream/reserve?consumer=invalid%20name")
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void thirdProcessingFailureMovesToDlqAndAcknowledgesOriginalOnce() {
        var event = new OrderEvent(UUID.randomUUID().toString(), "1", 1);
        String id = events.publish("stream", event).block().toString();
        stream.createGroup().block();
        String consumer = "retry-" + UUID.randomUUID();
        for (int attempt = 1; attempt <= 3; attempt++) {
            var rows = stream.process(consumer, attempt > 1, true).collectList().block();
            var row =
                    rows.stream()
                            .filter(value -> value.get("recordId").equals(id))
                            .findFirst()
                            .orElseThrow();
            assertEquals((long) attempt, row.get("attempts"));
            assertEquals(attempt < 3 ? "RETRY_PENDING" : "DEAD_LETTERED", row.get("status"));
            assertEquals(attempt < 3 ? 0L : 1L, row.get("acknowledged"));
        }
        assertTrue(
                stream.pending("0-0", 100).block().entries().stream()
                        .noneMatch(entry -> entry.recordId().equals(id)));
        var dlq = stream.deadLetters(100).collectList().block();
        assertEquals(
                1,
                dlq.stream()
                        .filter(
                                row ->
                                        ((Map<?, ?>) row.get("fields"))
                                                .get("originalRecordId")
                                                .equals(id))
                        .count());
        var fields =
                (Map<?, ?>)
                        dlq.stream()
                                .filter(
                                        row ->
                                                ((Map<?, ?>) row.get("fields"))
                                                        .get("originalRecordId")
                                                        .equals(id))
                                .findFirst()
                                .orElseThrow()
                                .get("fields");
        assertEquals("3", fields.get("attempts"));
        assertEquals("IllegalStateException", fields.get("errorType"));
        assertEquals(
                "NOT_PENDING",
                streamRepository
                        .recordFailure(id, consumer, 3, "IllegalStateException")
                        .block()
                        .status());
        assertFalse(redis.opsForHash().hasKey(STREAM_KEY + ":failures", id).block());
    }

    @Test
    void successfulRetryClearsFailureCounter() {
        var event = new OrderEvent(UUID.randomUUID().toString(), "1", 1);
        String id = events.publish("stream", event).block().toString();
        stream.createGroup().block();
        String consumer = "retry-success-" + UUID.randomUUID();
        var failed = stream.process(consumer, false, true).collectList().block();
        assertTrue(
                failed.stream()
                        .anyMatch(
                                row ->
                                        row.get("recordId").equals(id)
                                                && row.get("status").equals("RETRY_PENDING")));
        var retried = stream.process(consumer, true).collectList().block();
        assertTrue(
                retried.stream()
                        .anyMatch(
                                row ->
                                        row.get("recordId").equals(id)
                                                && row.get("status").equals("PROCESSED")));
        assertFalse(redis.opsForHash().hasKey(STREAM_KEY + ":failures", id).block());
        assertTrue(
                stream.deadLetters(100).collectList().block().stream()
                        .noneMatch(
                                row ->
                                        ((Map<?, ?>) row.get("fields"))
                                                .get("originalRecordId")
                                                .equals(id)));
    }

    @Test
    void dlqWriteFailureKeepsOriginalPending() {
        String key = "usecase:orders:dlq-failure:" + UUID.randomUUID();
        var repository = new OrderStreamRepository(redis, new Gson());
        ReflectionTestUtils.setField(repository, "key", key);
        var consumer = new OrderStreamConsumer(repository, new OrderEventProcessor());
        consumer.setMaxAttempts(1);
        String id =
                repository.publish(new OrderEvent(UUID.randomUUID().toString(), "1", 1)).block();
        repository.createGroup().block();
        redis.opsForValue().set(key + ":dlq", "wrong-type").block();
        StepVerifier.create(consumer.process("worker", false, true)).expectError().verify();
        assertTrue(
                repository.pending("0-0", 20).block().stream()
                        .anyMatch(message -> message.getIdAsString().equals(id)));
        assertEquals(1L, repository.failureCount(id).block());
        // DLQ 오류가 계속되어도 이미 소진한 실패 횟수는 증가하지 않는다.
        StepVerifier.create(consumer.process("worker", true)).expectError().verify();
        assertEquals(1L, repository.failureCount(id).block());
    }

    @Test
    void staleConsumerCannotCountFailuresOrDeadLetterNewOwnersMessage() {
        String id =
                events.publish("stream", new OrderEvent(UUID.randomUUID().toString(), "1", 1))
                        .block()
                        .toString();
        stream.createGroup().block();
        stream.reserve("current-owner").collectList().block();
        assertEquals(
                "OWNERSHIP_CHANGED",
                streamRepository.recordFailure(id, "stale-owner", 1, "Failure").block().status());
        assertFalse(redis.opsForHash().hasKey(STREAM_KEY + ":failures", id).block());
        stream.process("current-owner", true).collectList().block();
    }

    @Test
    void dlqHttpEndpointAndBounds() {
        client.get()
                .uri("/usecases/order-events/stream/dlq?count=20")
                .exchange()
                .expectStatus()
                .isOk();
        client.get()
                .uri("/usecases/order-events/stream/dlq?count=101")
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void httpFailureSimulationExhaustsBudgetAndExposesDlq() {
        client.post()
                .uri("/usecases/order-events/stream/publish")
                .bodyValue(new OrderEvent(UUID.randomUUID().toString(), "1", 1))
                .exchange()
                .expectStatus()
                .isOk();
        client.post().uri("/usecases/order-events/stream/group").exchange().expectStatus().isOk();
        String consumer = "http-dlq-" + UUID.randomUUID();
        for (int attempt = 1; attempt <= 3; attempt++) {
            client.post()
                    .uri(
                            "/usecases/order-events/stream/process?consumer="
                                    + consumer
                                    + "&pending="
                                    + (attempt > 1)
                                    + "&fail=true")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$[0].attempts")
                    .isEqualTo(attempt)
                    .jsonPath("$[0].status")
                    .isEqualTo(attempt < 3 ? "RETRY_PENDING" : "DEAD_LETTERED")
                    .jsonPath("$[0].acknowledged")
                    .isEqualTo(attempt < 3 ? 0 : 1);
        }
        client.get()
                .uri("/usecases/order-events/stream/dlq?count=100")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].fields.attempts")
                .isEqualTo("3");
    }
}
