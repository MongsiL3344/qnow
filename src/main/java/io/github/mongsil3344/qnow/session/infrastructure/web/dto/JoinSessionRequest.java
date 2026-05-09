package io.github.mongsil3344.qnow.session.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record JoinSessionRequest(
    @NotNull
    UUID userId
) {}
