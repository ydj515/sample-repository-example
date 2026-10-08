package com.example.quality.presentation.todo;

import com.example.quality.application.todo.TodoService;
import com.example.quality.domain.todo.Todo;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/todos")
public class TodoController {
  private final TodoService todoService;

  public TodoController(TodoService todoService) {
    this.todoService = todoService;
  }

  @GetMapping
  public List<Todo> findAll() {
    return todoService.findAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Todo create(@RequestBody CreateTodoRequest request) {
    return todoService.create(request.title());
  }

  public record CreateTodoRequest(String title) {}
}
