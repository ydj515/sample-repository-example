package com.example.jpajavaexample.infrastructure.persistence.user;

import com.example.jpajavaexample.domain.user.model.User;
import com.example.jpajavaexample.domain.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final JpaUserRepository jpaUserRepository;

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public User save(User user) {
        return jpaUserRepository.save(user);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return jpaUserRepository.findAll(pageable);
    }
}
