package com.example.quality.presentation.todo

import com.example.quality.application.todo.TodoService
import com.example.quality.domain.todo.Todo
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/todos")
class TodoController(
    private val todoService: TodoService,
) {
    @GetMapping
    fun findAll(): List<Todo> = todoService.findAll()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreateTodoRequest,
    ): Todo = todoService.create(request.title)

    data class CreateTodoRequest(
        val title: String?,
    )
}
