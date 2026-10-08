package com.example.quality.repository.todo;

import com.example.quality.domain.todo.Todo;
import com.example.quality.domain.todo.TodoRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTodoRepository extends JpaRepository<Todo, Long>, TodoRepository {}
