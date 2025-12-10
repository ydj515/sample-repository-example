package com.example.graphqlexample.graphql

import com.example.graphqlexample.graphql.input.AddCartItemInput
import com.example.graphqlexample.graphql.input.RemoveCartItemInput
import com.example.graphqlexample.graphql.dto.Cart as CartDto
import com.example.graphqlexample.service.CartService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class CartResolver(
    private val cartService: CartService,
    private val mapper: GraphqlMapper
) {
    @QueryMapping
    fun cart(@Argument userId: Long): CartDto =
        mapper.toCartDto(cartService.getCartForUser(userId))

    @MutationMapping
    fun addCartItem(@Argument input: AddCartItemInput): CartDto =
        mapper.toCartDto(cartService.addCartItem(input))

    @MutationMapping
    fun removeCartItem(@Argument input: RemoveCartItemInput): CartDto =
        mapper.toCartDto(cartService.removeCartItem(input))
}
