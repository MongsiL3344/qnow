package io.github.mongsil3344.qnow.presentation.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PresenterControlStore {

    void grant(UUID sessionId, UUID participantId, Instant expiresAt);

    boolean revoke(UUID sessionId, UUID participantId);

    Optional<Instant> getExpiry(UUID sessionId, UUID participantId);

    void clearSession(UUID sessionId);
}
