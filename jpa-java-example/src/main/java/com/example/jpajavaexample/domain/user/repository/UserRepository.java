package com.example.jpajavaexample.domain.user.repository;

import com.example.jpajavaexample.domain.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository {

    boolean existsByEmail(String email);

    User save(User user);

    Page<User> findAll(Pageable pageable);
}
