package io.github.mongsil3344.qnow.session.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateSessionRequest(
    @NotBlank
    String title,

    @NotNull
    Instant startAt
) {}
