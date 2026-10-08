package com.example.quality;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.quality.application.todo.TodoService;
import com.example.quality.domain.todo.InvalidTodoTitleException;
import com.example.quality.domain.todo.Todo;
import com.example.quality.domain.todo.TodoRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class TodoServiceTest {
  @Test
  void rejectsNullTitle() {
    TodoService service = new TodoService(mock(TodoRepository.class));
    assertThatThrownBy(() -> service.create(null))
        .isInstanceOf(InvalidTodoTitleException.class)
        .hasMessage("title must not be blank");
  }

  @Test
  void rejectsBlankTitle() {
    TodoService service = new TodoService(mock(TodoRepository.class));

    assertThatThrownBy(() -> service.create(" "))
        .isInstanceOf(InvalidTodoTitleException.class)
        .hasMessage("title must not be blank");
  }

  @Test
  void createsAndListsTodos() {
    TodoRepository repository = mock(TodoRepository.class);
    Todo todo = new Todo("write tests");
    when(repository.save(org.mockito.ArgumentMatchers.any(Todo.class))).thenReturn(todo);
    when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(todo));
    TodoService service = new TodoService(repository);

    org.assertj.core.api.Assertions.assertThat(service.create("write tests")).isSameAs(todo);
    org.assertj.core.api.Assertions.assertThat(service.findAll()).containsExactly(todo);
  }
}
