package io.github.mongsil3344.qnow.presentation.infrastructure.web.dto;

import io.github.mongsil3344.qnow.presentation.application.dto.PresenterViewResult;
import java.time.Instant;
import java.util.UUID;

public record PresenterViewResponse(
    boolean canControl,
    UUID sessionId,
    UUID presentationId,
    Integer pageNumber,
    long sequence,
    Instant updatedAt
) {

    public static PresenterViewResponse from(PresenterViewResult result) {
        return new PresenterViewResponse(
            result.canControl(),
            result.sessionId(),
            result.presentationId(),
            result.pageNumber(),
            result.sequence(),
            result.updatedAt()
        );
    }
}
