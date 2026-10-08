package com.example.quality

import com.example.quality.application.todo.TodoService
import com.example.quality.presentation.todo.TodoController
import com.example.quality.presentation.todo.TodoErrorHandler
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import jakarta.servlet.ServletException
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class TodoErrorHandlerTest :
    FunSpec({
        listOf(
            IllegalArgumentException("unexpected internal argument"),
            IllegalStateException("storage unavailable"),
        ).forEach { failure ->
            test("${failure.javaClass.simpleName} is not converted into a client error") {
                val service = Mockito.mock(TodoService::class.java)
                Mockito.`when`(service.create("valid")).thenThrow(failure)
                val mockMvc =
                    MockMvcBuilders
                        .standaloneSetup(TodoController(service))
                        .setControllerAdvice(TodoErrorHandler())
                        .build()

                shouldThrow<ServletException> {
                    mockMvc.post("/todos") {
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"title":"valid"}"""
                    }
                }.cause shouldBe failure
            }
        }
    })
