package com.example.jpajavaexample.infrastructure.persistence.user;

import com.example.jpajavaexample.domain.user.model.User;
import com.example.jpajavaexample.domain.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    public Page<User> search(String name, String email, Pageable pageable) {
        Specification<User> spec = Specification.where(null);

        if (StringUtils.hasText(name)) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), like(name)));
        }

        if (StringUtils.hasText(email)) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("email")), like(email)));
        }

        return jpaUserRepository.findAll(spec, pageable);
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

    private String like(String keyword) {
        return "%" + keyword.toLowerCase() + "%";
    }
}
