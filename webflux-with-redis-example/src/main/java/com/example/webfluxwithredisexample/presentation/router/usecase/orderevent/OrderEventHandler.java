package com.example.webfluxwithredisexample.presentation.router.usecase.orderevent;

import static com.example.webfluxwithredisexample.presentation.router.usecase.UsecaseResponses.ok;

import com.example.webfluxwithredisexample.application.usecase.orderevent.OrderEventService;
import com.example.webfluxwithredisexample.domain.usecase.orderevent.OrderEvent;
import com.example.webfluxwithredisexample.infrastructure.consumer.usecase.orderevent.*;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;

import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OrderEventHandler {
    private final OrderEventService service;
    private final OrderQueueConsumer queue;
    private final OrderPubSubSubscriber subscriber;
    private final OrderStreamConsumer stream;

    public Mono<ServerResponse> publish(ServerRequest req) {
        return ok(
                req.bodyToMono(OrderEvent.class)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Body required")))
                        .flatMap(event -> service.publish(req.pathVariable("transport"), event))
                        .map(result -> Map.of("result", result)));
    }

    public Mono<ServerResponse> next(ServerRequest req) {
        return ok(queue.next());
    }

    public Mono<ServerResponse> listen(ServerRequest req) {
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(subscriber.listen(), OrderEvent.class);
    }

    public Mono<ServerResponse> group(ServerRequest req) {
        return ok(stream.createGroup().map(result -> Map.of("group", result)));
    }

    public Mono<ServerResponse> process(ServerRequest req) {
        String consumer = req.queryParam("consumer").orElse("worker-1");
        if (consumer.isBlank()) {
            return Mono.error(new IllegalArgumentException("consumer required"));
        }

        return ok(stream.process(
                        consumer,
                        Boolean.parseBoolean(req.queryParam("pending").orElse("false")),
                        Boolean.parseBoolean(req.queryParam("fail").orElse("false")))
                .collectList());
    }

    public Mono<ServerResponse> reserve(ServerRequest req) {
        return ok(
                stream.reserve(req.queryParam("consumer").orElse("stopped-worker")).collectList());
    }

    public Mono<ServerResponse> pending(ServerRequest req) {
        return ok(stream.pending(
                req.queryParam("cursor").orElse("0-0"),
                Integer.parseInt(req.queryParam("count").orElse("20")))
        );
    }

    public Mono<ServerResponse> recover(ServerRequest req) {
        return ok(stream.recover(
                req.queryParam("consumer").orElse("recovery-worker"),
                Long.parseLong(req.queryParam("minIdleMillis").orElse("60000")),
                req.queryParam("cursor").orElse("0-0"),
                Integer.parseInt(req.queryParam("count").orElse("20")),
                Boolean.parseBoolean(req.queryParam("fail").orElse("false")))
        );
    }

    public Mono<ServerResponse> deadLetters(ServerRequest req) {
        return ok(stream.deadLetters(Integer.parseInt(req.queryParam("count").orElse("20")))
                .collectList()
        );
    }
}
