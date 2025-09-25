package com.example.jpajavaexample.application.user;

import com.example.jpajavaexample.domain.user.repository.UserRepository;
import com.example.jpajavaexample.presentation.user.dto.UserDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<UserDetailResponse> getUsers(String name, String email, Pageable pageable) {
        return userRepository.search(name, email, pageable)
            .map(UserDetailResponse::from);
    }
}
