package com.example.graphqlexample.graphql

import com.example.graphqlexample.domain.Cart
import com.example.graphqlexample.domain.CartItem
import com.example.graphqlexample.domain.Clothing
import com.example.graphqlexample.domain.Electronics
import com.example.graphqlexample.domain.Product
import com.example.graphqlexample.domain.User
import com.example.graphqlexample.graphql.dto.Cart as CartDto
import com.example.graphqlexample.graphql.dto.CartItem as CartItemDto
import com.example.graphqlexample.graphql.dto.Clothing as ClothingDto
import com.example.graphqlexample.graphql.dto.Electronics as ElectronicsDto
import com.example.graphqlexample.graphql.dto.Product as ProductDto
import com.example.graphqlexample.graphql.dto.User as UserDto
import com.example.graphqlexample.service.CartService
import org.springframework.stereotype.Component

@Component
class GraphqlMapper(
    private val cartService: CartService
) {
    fun toUserDto(user: User): UserDto =
        UserDto(
            id = user.id ?: error("User id is null"),
            name = user.name,
            email = user.email,
            createdAt = user.createdAt,
            cart = null
        )

    fun toProductDto(product: Product): ProductDto =
        when (product) {
            is Electronics -> toElectronicsDto(product)
            is Clothing -> toClothingDto(product)
            else -> throw IllegalArgumentException("Unsupported product type: ${product::class.simpleName}")
        }

    fun toElectronicsDto(electronics: Electronics): ElectronicsDto =
        ElectronicsDto(
            id = electronics.id ?: error("Electronics id is null"),
            name = electronics.name,
            price = electronics.price,
            productType = electronics.productType,
            warrantyPeriod = electronics.warrantyPeriod
        )

    fun toClothingDto(clothing: Clothing): ClothingDto =
        ClothingDto(
            id = clothing.id ?: error("Clothing id is null"),
            name = clothing.name,
            price = clothing.price,
            productType = clothing.productType,
            size = clothing.size
        )

    fun toCartItemDto(cartItem: CartItem): CartItemDto =
        CartItemDto(
            id = cartItem.id ?: error("CartItem id is null"),
            product = toProductDto(cartItem.product),
            quantity = cartItem.quantity
        )

    fun toCartDto(cart: Cart): CartDto =
        CartDto(
            id = cart.id ?: error("Cart id is null"),
            user = toUserDto(cart.user),
            items = cart.items.map { toCartItemDto(it) },
            totalAmount = cartService.calculateTotalAmount(cart)
        )

    fun toSearchResultDto(result: Any): com.example.graphqlexample.graphql.dto.SearchResult =
        when (result) {
            is User -> toUserDto(result)
            is Product -> toProductDto(result)
            else -> throw IllegalArgumentException("Unsupported search result type: ${result::class.simpleName}")
        }
}
