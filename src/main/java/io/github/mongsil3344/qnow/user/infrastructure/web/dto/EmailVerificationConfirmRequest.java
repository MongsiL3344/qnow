package io.github.mongsil3344.qnow.user.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailVerificationConfirmRequest(
    @NotBlank(message = "이메일은 비어 있을 수 없습니다")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    @Size(max = 255, message = "이메일은 255자를 넘을 수 없습니다")
    String email,

    @NotBlank(message = "인증번호는 비어 있을 수 없습니다")
    @Pattern(regexp = "\\d{6}", message = "인증번호는 6자리 숫자여야 합니다")
    String verificationCode
) {}
