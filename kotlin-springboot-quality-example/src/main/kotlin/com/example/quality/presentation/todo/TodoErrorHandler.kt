package com.example.quality.presentation.todo

import com.example.quality.domain.todo.InvalidTodoTitleException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [TodoController::class])
class TodoErrorHandler {
    @ExceptionHandler(InvalidTodoTitleException::class)
    fun invalidTitle(): ProblemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "title must not be blank")

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun unreadableBody(): ProblemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "request body must be valid JSON")
}
