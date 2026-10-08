package com.example.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.quality.application.todo.TodoService;
import com.example.quality.domain.todo.Todo;
import com.example.quality.presentation.todo.TodoController;
import java.util.List;
import org.junit.jupiter.api.Test;

class TodoControllerTest {
  @Test
  void delegatesTodoRequestsToService() {
    TodoService service = mock(TodoService.class);
    Todo todo = new Todo("learn quality gates");
    when(service.findAll()).thenReturn(List.of(todo));
    when(service.create("learn quality gates")).thenReturn(todo);
    TodoController controller = new TodoController(service);

    assertThat(controller.findAll()).containsExactly(todo);
    assertThat(controller.create(new TodoController.CreateTodoRequest("learn quality gates")))
        .isSameAs(todo);
  }
}
