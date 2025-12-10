package com.example.graphqlexample.repository

import com.example.graphqlexample.domain.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByNameContainingIgnoreCase(keyword: String): List<User>
    fun existsByEmail(email: String): Boolean
}
