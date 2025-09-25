package com.example.jpajavaexample.presentation.user;

import com.example.jpajavaexample.application.user.UserQueryService;
import com.example.jpajavaexample.application.user.UserSignupService;
import com.example.jpajavaexample.presentation.user.dto.UserDetailResponse;
import com.example.jpajavaexample.presentation.user.dto.UserSignupRequest;
import com.example.jpajavaexample.presentation.user.dto.UserSearchRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserSignupService userSignupService;
    private final UserQueryService userQueryService;

    @PostMapping
    public ResponseEntity<UserDetailResponse> signup(@Valid @RequestBody UserSignupRequest request) {
        UserDetailResponse response = userSignupService.signup(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<UserDetailResponse>> getUsers(
        @ModelAttribute UserSearchRequest searchRequest,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<UserDetailResponse> users = userQueryService.getUsers(
            searchRequest.name(),
            searchRequest.email(),
            pageable
        );
        return ResponseEntity.ok(users);
    }
}
