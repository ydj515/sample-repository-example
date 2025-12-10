package com.example.graphqlexample.graphql

import com.example.graphqlexample.graphql.dto.Product as ProductDto
import com.example.graphqlexample.service.ProductEventPublisher
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux

@Controller
class SubscriptionResolver(
    private val productEventPublisher: ProductEventPublisher,
    private val mapper: GraphqlMapper
) {
    @SubscriptionMapping
    fun newProduct(@Argument keyword: String?): Flux<ProductDto> {
        val filterKeyword = keyword?.takeIf { it.isNotBlank() }?.lowercase()
        return productEventPublisher.flux()
            .filter { product ->
                filterKeyword?.let { product.name.contains(it, ignoreCase = true) } ?: true
            }
            .map { mapper.toProductDto(it) }
    }
}
