package com.example.jpajavaexample.presentation.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserSignupRequest(
    @NotBlank(message = "이름은 필수 값입니다.")
    @Size(max = 100, message = "이름은 100자를 넘을 수 없습니다.")
    String name,

    @NotBlank(message = "이메일은 필수 값입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 150, message = "이메일은 150자를 넘을 수 없습니다.")
    String email,

    @NotBlank(message = "비밀번호는 필수 값입니다.")
    @Size(min = 6, max = 100, message = "비밀번호는 6자 이상 100자 이하로 입력해주세요.")
    String password
) {
}
