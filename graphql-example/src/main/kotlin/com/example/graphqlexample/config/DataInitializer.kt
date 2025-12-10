package com.example.graphqlexample.config

import com.example.graphqlexample.domain.ProductType
import com.example.graphqlexample.graphql.input.AddCartItemInput
import com.example.graphqlexample.graphql.input.AddProductInput
import com.example.graphqlexample.graphql.input.CreateUserInput
import com.example.graphqlexample.repository.UserRepository
import com.example.graphqlexample.service.CartService
import com.example.graphqlexample.service.ProductService
import com.example.graphqlexample.service.UserService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class DataInitializer(
    private val userService: UserService,
    private val productService: ProductService,
    private val cartService: CartService,
    private val userRepository: UserRepository
) : CommandLineRunner {
    override fun run(vararg args: String) {
        if (userRepository.count() > 0) {
            return
        }

        // Products
        val macbook = productService.addProduct(
            AddProductInput(
                name = "mac book air",
                price = 1000,
                productType = ProductType.ELECTRONICS,
                warrantyPeriod = 36
            )
        )
        val iphone = productService.addProduct(
            AddProductInput(
                name = "iphone 19",
                price = 2000,
                productType = ProductType.ELECTRONICS,
                warrantyPeriod = 24
            )
        )
        val tv = productService.addProduct(
            AddProductInput(
                name = "samsung tv",
                price = 3000,
                productType = ProductType.ELECTRONICS,
                warrantyPeriod = 12
            )
        )
        val tshirt = productService.addProduct(
            AddProductInput(
                name = "T-shirt",
                price = 300,
                productType = ProductType.CLOTHING,
                size = "M"
            )
        )
        val jeans = productService.addProduct(
            AddProductInput(
                name = "Jeans",
                price = 200,
                productType = ProductType.CLOTHING,
                size = "L"
            )
        )
        val dress = productService.addProduct(
            AddProductInput(
                name = "Dress",
                price = 100,
                productType = ProductType.CLOTHING,
                size = "S"
            )
        )

        // Users with custom createdAt
        val ydjUser = userService.createUser(CreateUserInput(name = "ydj", email = "ydj@example.com"))
        ydjUser.createdAt = OffsetDateTime.now().minusDays(30)
        userRepository.save(ydjUser)

        val dongjinUser = userService.createUser(CreateUserInput(name = "dongjin", email = "dongjin@example.com"))
        dongjinUser.createdAt = OffsetDateTime.now().minusDays(60)
        userRepository.save(dongjinUser)

        // Cart items for ydj
        cartService.addCartItem(
            AddCartItemInput(
                userId = ydjUser.id!!,
                productId = macbook.id!!,
                quantity = 1
            )
        )
        cartService.addCartItem(
            AddCartItemInput(
                userId = ydjUser.id!!,
                productId = iphone.id!!,
                quantity = 4
            )
        )
        cartService.addCartItem(
            AddCartItemInput(
                userId = ydjUser.id!!,
                productId = dress.id!!,
                quantity = 6
            )
        )

        // Cart items for dongjin
        cartService.addCartItem(
            AddCartItemInput(
                userId = dongjinUser.id!!,
                productId = tv.id!!,
                quantity = 2
            )
        )
        cartService.addCartItem(
            AddCartItemInput(
                userId = dongjinUser.id!!,
                productId = tshirt.id!!,
                quantity = 4
            )
        )
        cartService.addCartItem(
            AddCartItemInput(
                userId = dongjinUser.id!!,
                productId = jeans.id!!,
                quantity = 3
            )
        )
    }
}
