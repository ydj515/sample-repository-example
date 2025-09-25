package com.example.jpajavaexample.domain.user.repository;

import com.example.jpajavaexample.domain.user.model.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository {

    Page<User> search(String name, String email, Pageable pageable);

    boolean existsByEmail(String email);

    User save(User user);

    Page<User> findAll(Pageable pageable);

    List<User> findAll();
}
