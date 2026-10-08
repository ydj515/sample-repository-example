package com.example.quality.repository.todo

import com.example.quality.domain.todo.Todo
import com.example.quality.domain.todo.TodoRepository
import org.springframework.data.jpa.repository.JpaRepository

interface JpaTodoRepository :
    JpaRepository<Todo, Long>,
    TodoRepository
