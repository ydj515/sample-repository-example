package com.example.quality

import com.example.quality.application.todo.TodoService
import com.example.quality.domain.todo.InvalidTodoTitleException
import com.example.quality.domain.todo.Todo
import com.example.quality.domain.todo.TodoRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class TodoServiceTest :
    FunSpec({
        test("create saves the supplied title") {
            val repository = InMemoryTodoRepository()
            val service = TodoService(repository)

            val saved = service.create("learn quality gates")

            saved.title shouldBe "learn quality gates"
            service.findAll() shouldBe listOf(saved)
        }

        test("findAll returns an empty list before creation") {
            TodoService(InMemoryTodoRepository()).findAll().shouldBeEmpty()
        }

        listOf(null, "", " ", "\t\n").forEachIndexed { index, invalidTitle ->
            test("create rejects invalid title case $index without saving") {
                val repository = InMemoryTodoRepository()
                val service = TodoService(repository)

                shouldThrow<InvalidTodoTitleException> {
                    service.create(invalidTitle)
                }.message shouldBe "title must not be blank"
                repository.findAllByOrderByIdAsc().shouldBeEmpty()
            }
        }

        test("create preserves repository failures") {
            val repository =
                object : TodoRepository {
                    override fun <S : Todo> save(todo: S): S = error("storage unavailable")

                    override fun findAllByOrderByIdAsc(): List<Todo> = emptyList()
                }

            shouldThrow<IllegalStateException> {
                TodoService(repository).create("valid")
            }.message shouldBe "storage unavailable"
        }
    })

private class InMemoryTodoRepository : TodoRepository {
    private val todos = mutableListOf<Todo>()

    override fun <S : Todo> save(todo: S): S {
        todos.add(todo)
        return todo
    }

    override fun findAllByOrderByIdAsc(): List<Todo> = todos.toList()
}
