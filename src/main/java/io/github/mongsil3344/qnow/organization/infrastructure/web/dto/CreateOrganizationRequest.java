package io.github.mongsil3344.qnow.organization.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
    @NotBlank(message = "조직명은 비어있을 수 없습니다")
    @Size(max=30, message = "조직명은 30자를 넘을 수 없습니다")
    String name,

    @Size(max = 255, message = "설명은 255자를 넘을 수 없습니다")
    String detail,

    @Size(min = 8, max = 255, message = "비밀번호는 8자 이상 255자 이하여야 합니다")
    @Pattern(regexp = ".*\\S.*", message = "비밀번호는 공백만으로 이루어질 수 없습니다")
    String password
) {}
