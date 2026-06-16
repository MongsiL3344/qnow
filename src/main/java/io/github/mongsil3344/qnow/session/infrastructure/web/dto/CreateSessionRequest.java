package io.github.mongsil3344.qnow.session.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSessionRequest(
    @NotBlank
    String title
) {}
