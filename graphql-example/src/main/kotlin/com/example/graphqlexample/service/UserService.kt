package com.example.graphqlexample.service

import com.example.graphqlexample.domain.User
import com.example.graphqlexample.exception.InvalidInputException
import com.example.graphqlexample.exception.NotFoundException
import com.example.graphqlexample.graphql.input.CreateUserInput
import com.example.graphqlexample.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class UserService(
    private val userRepository: UserRepository
) {
    fun getUser(userId: Long): User =
        userRepository.findById(userId).orElseThrow { NotFoundException("User not found with id $userId") }

    fun createUser(input: CreateUserInput): User {
        if (input.name.isBlank()) {
            throw InvalidInputException("Name must not be blank")
        }
        if (input.email.isBlank()) {
            throw InvalidInputException("Email must not be blank")
        }
        if (userRepository.existsByEmail(input.email)) {
            throw InvalidInputException("Email already exists")
        }

        val user = User(
            name = input.name.trim(),
            email = input.email.trim(),
            createdAt = OffsetDateTime.now()
        )
        return userRepository.save(user)
    }

    fun search(keyword: String): List<User> =
        userRepository.findByNameContainingIgnoreCase(keyword)
}
