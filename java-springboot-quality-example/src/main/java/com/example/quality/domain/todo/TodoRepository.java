package com.example.quality.domain.todo;

import java.util.List;

public interface TodoRepository {
  <S extends Todo> S save(S todo);

  List<Todo> findAllByOrderByIdAsc();
}
