package io.github.mongsil3344.qnow.presentation.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PresenterViewEvent(
    PresenterViewEventType type,
    UUID sessionId,
    UUID presentationId,
    Integer pageNumber,
    long revision,
    Instant updatedAt,
    PresenterViewClearReason reason
) {

    public PresenterViewEvent {
        Objects.requireNonNull(type);
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(updatedAt);
        if (revision < 1) {
            throw new IllegalArgumentException("event revision must be positive");
        }
        if ((presentationId == null) != (pageNumber == null)) {
            throw new IllegalArgumentException("presentationId and pageNumber must both be present or absent");
        }
        if (type == PresenterViewEventType.PRESENTER_VIEW_UPDATED
            && (presentationId == null || reason != null)) {
            throw new IllegalArgumentException("updated event requires a presenter view without a clear reason");
        }
        if (type == PresenterViewEventType.PRESENTER_VIEW_CLEARED
            && (presentationId != null || reason == null)) {
            throw new IllegalArgumentException("cleared event requires no presenter view and a clear reason");
        }
    }

    public PresenterViewSnapshot toSnapshot() {
        return new PresenterViewSnapshot(sessionId, presentationId, pageNumber, revision, updatedAt);
    }
}
