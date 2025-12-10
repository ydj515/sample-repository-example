package com.example.graphqlexample.service

import com.example.graphqlexample.domain.Cart
import com.example.graphqlexample.domain.CartItem
import com.example.graphqlexample.domain.User
import com.example.graphqlexample.exception.InvalidInputException
import com.example.graphqlexample.exception.NotFoundException
import com.example.graphqlexample.graphql.input.AddCartItemInput
import com.example.graphqlexample.graphql.input.RemoveCartItemInput
import com.example.graphqlexample.repository.CartItemRepository
import com.example.graphqlexample.repository.CartRepository
import com.example.graphqlexample.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CartService(
    private val cartRepository: CartRepository,
    private val cartItemRepository: CartItemRepository,
    private val userService: UserService,
    private val productRepository: ProductRepository
) {
    @Transactional
    fun getCartForUser(userId: Long): Cart {
        val user = userService.getUser(userId)
        return cartRepository.findByUserId(user.id!!) ?: createCartForUser(user)
    }

    @Transactional
    fun addCartItem(input: AddCartItemInput): Cart {
        if (input.quantity <= 0) {
            throw InvalidInputException("Quantity must be positive")
        }
        val user = userService.getUser(input.userId)
        val product = productRepository.findById(input.productId)
            .orElseThrow { NotFoundException("Product not found with id ${input.productId}") }

        val cart = cartRepository.findByUserId(user.id!!) ?: createCartForUser(user)
        val existingItem = cart.items.firstOrNull { it.product.id == product.id }
        if (existingItem != null) {
            existingItem.quantity += input.quantity
        } else {
            cart.items.add(CartItem(product = product, cart = cart, quantity = input.quantity))
        }

        return cartRepository.save(cart)
    }

    @Transactional
    fun removeCartItem(input: RemoveCartItemInput): Cart {
        val user = userService.getUser(input.userId)
        val cart = cartRepository.findByUserId(user.id!!)
            ?: throw NotFoundException("Cart not found for user ${input.userId}")

        val targetItem = cart.items.firstOrNull { it.id == input.cartItemId }
            ?: throw NotFoundException("Cart item not found with id ${input.cartItemId}")

        cart.items.remove(targetItem)
        cartItemRepository.delete(targetItem)
        return cartRepository.save(cart)
    }

    fun calculateTotalAmount(cart: Cart): Int =
        cart.items.sumOf { it.product.price * it.quantity }

    private fun createCartForUser(user: User): Cart {
        val cart = Cart(user = user)
        user.cart = cart
        return cartRepository.save(cart)
    }
}
