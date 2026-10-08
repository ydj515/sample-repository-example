package com.example.quality.domain.todo

interface TodoRepository {
    fun <S : Todo> save(todo: S): S

    fun findAllByOrderByIdAsc(): List<Todo>
}
