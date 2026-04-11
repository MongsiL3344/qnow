package io.github.mongsil3344.qnow.user.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
    @NotBlank(message = "이메일은 비어 있을 수 없습니다")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    @Size(max = 255, message = "이메일은 255자를 넘을 수 없습니다")
    String email,

    @NotBlank(message = "닉네임은 비어 있을 수 없습니다")
    @Size(max = 30, message = "닉네임은 30자를 넘을 수 없습니다")
    String nickname,

    @NotBlank(message = "아이디는 비어 있을 수 없습니다")
    @Size(max = 30, message = "아이디는 30자를 넘을 수 없습니다")
    String username,

    @NotBlank(message = "비밀번호는 비어 있을 수 없습니다")
    @Size(min = 8, max = 255, message = "비밀번호는 8자 이상 255자 이하여야 합니다")
    String password
) {}
