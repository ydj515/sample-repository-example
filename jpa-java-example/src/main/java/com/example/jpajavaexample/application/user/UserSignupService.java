package com.example.jpajavaexample.application.user;

import com.example.jpajavaexample.domain.user.model.User;
import com.example.jpajavaexample.domain.user.repository.UserRepository;
import com.example.jpajavaexample.domain.user.service.UserValidator;
import com.example.jpajavaexample.presentation.user.dto.UserDetailResponse;
import com.example.jpajavaexample.presentation.user.dto.UserSignupRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserSignupService {

    private final UserRepository userRepository;
    private final UserValidator userValidator;

    @Transactional
    public UserDetailResponse signup(UserSignupRequest request) {
        userValidator.ensureEmailNotDuplicated(request.email());

        User user = User.create(request.name(), request.email(), request.password());
        User saved = userRepository.save(user);

        return UserDetailResponse.from(saved);
    }
}
