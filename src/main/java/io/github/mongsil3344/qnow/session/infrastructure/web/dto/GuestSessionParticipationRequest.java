package io.github.mongsil3344.qnow.session.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GuestSessionParticipationRequest(
    @NotBlank(message = "참가 코드를 입력해주세요")
    @Pattern(
        regexp = "(?i)[A-HJ-KM-NP-Z2-9]{4}-?[A-HJ-KM-NP-Z2-9]{4}",
        message = "참가 코드 형식이 올바르지 않습니다"
    )
    String code,

    @NotBlank(message = "닉네임을 입력해주세요")
    @Size(max = 30, message = "닉네임은 30자 이하여야 합니다")
    String nickname
) {
}
