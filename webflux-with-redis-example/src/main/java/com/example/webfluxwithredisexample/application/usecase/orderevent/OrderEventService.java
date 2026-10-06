package com.example.webfluxwithredisexample.application.usecase.orderevent;

import com.example.webfluxwithredisexample.domain.usecase.orderevent.OrderEvent;
import com.example.webfluxwithredisexample.infrastructure.repository.usecase.orderevent.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OrderEventService {
    private final OrderQueueRepository queue;

    private final OrderPubSubRepository pubsub;

    private final OrderStreamRepository stream;

    public Mono<?> publish(String transport, OrderEvent event) {
        if (event.orderId() == null
                || event.orderId().isBlank()
                || event.productId() == null
                || event.productId().isBlank()
                || event.quantity() < 1)
            return Mono.error(
                    new IllegalArgumentException(
                            "orderId, productId and positive quantity required"));
        return switch (transport) {
            case "list" -> queue.publish(event);
            case "pubsub" -> pubsub.publish(event);
            case "stream" -> stream.publish(event);
            default ->
                    Mono.error(
                            new IllegalArgumentException(
                                    "transport must be list, pubsub or stream"));
        };
    }
}
