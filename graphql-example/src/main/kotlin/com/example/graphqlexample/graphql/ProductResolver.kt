package com.example.graphqlexample.graphql

import com.example.graphqlexample.graphql.input.AddProductInput
import com.example.graphqlexample.graphql.dto.Product as ProductDto
import com.example.graphqlexample.service.ProductService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class ProductResolver(
    private val productService: ProductService,
    private val mapper: GraphqlMapper
) {
    @QueryMapping
    fun productList(): List<ProductDto> =
        productService.getAllProducts().map { mapper.toProductDto(it) }

    @MutationMapping
    fun addProduct(@Argument input: AddProductInput): ProductDto =
        mapper.toProductDto(productService.addProduct(input))
}
