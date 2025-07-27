package com.example.localcacheexample

import com.example.localcacheexample.domain.product.Product
import com.example.localcacheexample.domain.product.repository.ProductRepository
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class SpringEventHandler(
    private val productRepository: ProductRepository
) {
    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReadyEvent(event: ApplicationReadyEvent?) {
        val names = listOf("A", "B", "C", "D", "E", "F", "G")
        val products = (1..10000).map { i ->
            val name = names[i % names.size] // A ~ G 반복
            Product(name = name, price = BigDecimal.ONE)
        }

        productRepository.saveAll(products)
    }
}