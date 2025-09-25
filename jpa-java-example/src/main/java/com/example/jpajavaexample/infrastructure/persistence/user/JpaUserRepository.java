package com.example.jpajavaexample.infrastructure.persistence.user;

import com.example.jpajavaexample.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
}
