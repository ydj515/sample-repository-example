package com.example.quality.application.todo

import com.example.quality.domain.todo.Todo
import com.example.quality.domain.todo.TodoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoService(
    private val todoRepository: TodoRepository,
) {
    @Transactional
    fun create(title: String?): Todo {
        require(!title.isNullOrBlank()) { "title must not be blank" }
        return todoRepository.save(Todo(title))
    }

    @Transactional(readOnly = true)
    fun findAll(): List<Todo> = todoRepository.findAllByOrderByIdAsc()
}
