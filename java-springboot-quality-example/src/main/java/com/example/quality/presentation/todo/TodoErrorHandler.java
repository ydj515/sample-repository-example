package com.example.quality.presentation.todo;

import com.example.quality.domain.todo.InvalidTodoTitleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = TodoController.class)
public class TodoErrorHandler {
  @ExceptionHandler(InvalidTodoTitleException.class)
  public ProblemDetail handleInvalidTitle(InvalidTodoTitleException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    problem.setTitle("Invalid todo title");
    problem.setProperty("code", "INVALID_TODO_TITLE");
    return problem;
  }
}
