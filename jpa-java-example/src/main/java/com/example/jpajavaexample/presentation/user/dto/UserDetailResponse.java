package com.example.jpajavaexample.presentation.user.dto;

import com.example.jpajavaexample.domain.user.model.User;

import java.time.LocalDateTime;

public record UserDetailResponse(
    Long id,
    String name,
    String email,
    LocalDateTime createdAt
) {
    public static UserDetailResponse from(User user) {
        return new UserDetailResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getCreatedAt()
        );
    }
}
