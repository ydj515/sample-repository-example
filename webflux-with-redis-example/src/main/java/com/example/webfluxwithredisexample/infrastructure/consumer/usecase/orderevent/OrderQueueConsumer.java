package com.example.webfluxwithredisexample.infrastructure.consumer.usecase.orderevent;

import com.example.webfluxwithredisexample.application.usecase.orderevent.OrderEventProcessor;
import com.example.webfluxwithredisexample.domain.usecase.orderevent.OrderEvent;
import com.example.webfluxwithredisexample.infrastructure.repository.usecase.orderevent.OrderQueueRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import reactor.core.publisher.*;

@Component
@RequiredArgsConstructor
public class OrderQueueConsumer {
    private final OrderQueueRepository repository;
    private final OrderEventProcessor processor;

    public Mono<OrderEvent> next() {
        return repository.next().flatMap(processor::process);
    }
}
