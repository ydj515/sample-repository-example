package com.example.graphqlexample.graphql

import com.example.graphqlexample.graphql.input.CreateUserInput
import com.example.graphqlexample.graphql.dto.Cart as CartDto
import com.example.graphqlexample.graphql.dto.User as UserDto
import com.example.graphqlexample.service.CartService
import com.example.graphqlexample.service.UserService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class UserResolver(
    private val userService: UserService,
    private val cartService: CartService,
    private val mapper: GraphqlMapper
) {
    @QueryMapping
    fun user(@Argument id: Long): UserDto =
        mapper.toUserDto(userService.getUser(id))

    @MutationMapping
    fun createUser(@Argument input: CreateUserInput): UserDto =
        mapper.toUserDto(userService.createUser(input))

    @SchemaMapping(typeName = "User", field = "cart")
    fun userCart(user: UserDto): CartDto =
        mapper.toCartDto(cartService.getCartForUser(user.id))
}
