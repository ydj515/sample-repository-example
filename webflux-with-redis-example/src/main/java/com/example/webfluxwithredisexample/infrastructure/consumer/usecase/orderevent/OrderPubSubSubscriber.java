package com.example.webfluxwithredisexample.infrastructure.consumer.usecase.orderevent;

import com.example.webfluxwithredisexample.application.usecase.orderevent.OrderEventProcessor;
import com.example.webfluxwithredisexample.domain.usecase.orderevent.OrderEvent;
import com.example.webfluxwithredisexample.infrastructure.repository.usecase.orderevent.OrderPubSubRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import reactor.core.publisher.*;

@Component
@RequiredArgsConstructor
public class OrderPubSubSubscriber {
    private final OrderPubSubRepository repository;
    private final OrderEventProcessor processor;

    public Flux<OrderEvent> listen() {
        return repository.listen().concatMap(processor::process);
    }
}
