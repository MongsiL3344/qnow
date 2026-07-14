package io.github.mongsil3344.qnow.presentation.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PresenterViewSnapshot(
    UUID sessionId,
    UUID presentationId,
    Integer pageNumber,
    long revision,
    Instant updatedAt
) {

    public PresenterViewSnapshot {
        Objects.requireNonNull(sessionId);
        if ((presentationId == null) != (pageNumber == null)) {
            throw new IllegalArgumentException("presentationId and pageNumber must both be present or absent");
        }
        if (pageNumber != null && pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be positive");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }
        if (revision == 0 && (presentationId != null || updatedAt != null)) {
            throw new IllegalArgumentException("revision zero is reserved for an empty snapshot");
        }
        if (revision > 0 && updatedAt == null) {
            throw new IllegalArgumentException("revisioned snapshot requires updatedAt");
        }
    }

    public static PresenterViewSnapshot empty(UUID sessionId) {
        return new PresenterViewSnapshot(sessionId, null, null, 0, null);
    }

    public boolean hasView() {
        return presentationId != null;
    }
}
