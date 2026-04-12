package io.github.mongsil3344.qnow.session.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateSessionRequest(

    @NotNull
    UUID creatorId,

    @NotBlank
    String title
) {}
