package com.example.jpajavaexample.domain.user.service;

import com.example.jpajavaexample.domain.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserValidator {

    private final UserRepository userRepository;

    public void ensureEmailNotDuplicated(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateUserEmailException(email);
        }
    }
}
