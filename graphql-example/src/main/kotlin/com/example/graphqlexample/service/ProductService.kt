package com.example.graphqlexample.service

import com.example.graphqlexample.domain.Clothing
import com.example.graphqlexample.domain.Electronics
import com.example.graphqlexample.domain.Product
import com.example.graphqlexample.domain.ProductType
import com.example.graphqlexample.exception.InvalidInputException
import com.example.graphqlexample.graphql.input.AddProductInput
import com.example.graphqlexample.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val productEventPublisher: ProductEventPublisher
) {
    @Transactional(readOnly = true)
    fun getAllProducts(): List<Product> = productRepository.findAll()

    @Transactional
    fun addProduct(input: AddProductInput): Product {
        validateProductInput(input)

        val product: Product = when (input.productType) {
            ProductType.ELECTRONICS -> Electronics(
                name = input.name.trim(),
                price = input.price,
                warrantyPeriod = input.warrantyPeriod!!
            )

            ProductType.CLOTHING -> Clothing(
                name = input.name.trim(),
                price = input.price,
                size = input.size!!.trim()
            )
        }

        val saved = productRepository.save(product)
        productEventPublisher.emit(saved)
        return saved
    }

    @Transactional(readOnly = true)
    fun search(keyword: String): List<Product> =
        productRepository.findByNameContainingIgnoreCase(keyword)

    private fun validateProductInput(input: AddProductInput) {
        if (input.name.isBlank()) {
            throw InvalidInputException("Product name must not be blank")
        }
        if (input.price < 0) {
            throw InvalidInputException("Product price must be zero or positive")
        }
        when (input.productType) {
            ProductType.ELECTRONICS -> {
                val warranty = input.warrantyPeriod
                    ?: throw InvalidInputException("warrantyPeriod is required for electronics")
                if (warranty <= 0) {
                    throw InvalidInputException("warrantyPeriod must be positive")
                }
            }

            ProductType.CLOTHING -> {
                val size = input.size
                    ?: throw InvalidInputException("size is required for clothing")
                if (size.isBlank()) {
                    throw InvalidInputException("size must not be blank")
                }
            }
        }
    }
}
