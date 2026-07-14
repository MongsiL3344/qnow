package io.github.mongsil3344.qnow.presentation.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdatePresenterViewRequest(
    @NotNull UUID presentationId,
    @NotNull Integer pageNumber
) {
}
