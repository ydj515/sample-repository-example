package com.example.quality.domain.todo;

public class InvalidTodoTitleException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public InvalidTodoTitleException() {
    super("title must not be blank");
  }
}
