package com.example.graphqlexample.service

import com.example.graphqlexample.domain.Product
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

@Component
class ProductEventPublisher {
    private val sink: Sinks.Many<Product> = Sinks.many().multicast().directBestEffort()

    fun emit(product: Product) {
        sink.tryEmitNext(product)
    }

    fun flux(): Flux<Product> = sink.asFlux()
}
