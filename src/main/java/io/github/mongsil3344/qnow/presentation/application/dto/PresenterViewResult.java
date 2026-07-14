package io.github.mongsil3344.qnow.presentation.application.dto;

import io.github.mongsil3344.qnow.presentation.domain.PresenterViewSnapshot;
import java.time.Instant;
import java.util.UUID;

public record PresenterViewResult(
    boolean canControl,
    UUID sessionId,
    UUID presentationId,
    Integer pageNumber,
    long revision,
    Instant updatedAt
) {

    public static PresenterViewResult from(boolean canControl, PresenterViewSnapshot snapshot) {
        return new PresenterViewResult(
            canControl,
            snapshot.sessionId(),
            snapshot.presentationId(),
            snapshot.pageNumber(),
            snapshot.revision(),
            snapshot.updatedAt()
        );
    }
}
