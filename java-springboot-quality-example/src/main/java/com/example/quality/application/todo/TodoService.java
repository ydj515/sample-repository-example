package com.example.quality.application.todo;

import com.example.quality.domain.todo.Todo;
import com.example.quality.domain.todo.TodoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TodoService {
  private final TodoRepository todoRepository;

  public TodoService(TodoRepository todoRepository) {
    this.todoRepository = todoRepository;
  }

  @Transactional
  public Todo create(String title) {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("title must not be blank");
    }
    return todoRepository.save(new Todo(title));
  }

  @Transactional(readOnly = true)
  public List<Todo> findAll() {
    return todoRepository.findAllByOrderByIdAsc();
  }
}
