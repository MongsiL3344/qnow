package io.github.mongsil3344.qnow.organization.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record JoinOrganizationRequest(
    @NotNull
    UUID userId,

    @Size(min = 8, max = 255, message = "비밀번호는 8자 이상 255자 이하여야 합니다")
    @Pattern(regexp = ".*\\S.*", message = "비밀번호는 공백만으로 이루어질 수 없습니다")
    String password
) {

    // 만약 비밀번호가 공백으로 들어오면 null로 치환
    public JoinOrganizationRequest {
        if (password != null && password.isBlank()) {
            password = null;
        }
    }
}
