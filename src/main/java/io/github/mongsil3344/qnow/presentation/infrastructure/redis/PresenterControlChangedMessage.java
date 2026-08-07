package io.github.mongsil3344.qnow.presentation.infrastructure.redis;

import java.time.Instant;
import java.util.UUID;

record PresenterControlChangedMessage(
    String type,
    UUID sessionId,
    Instant occurredAt
) {

    static final String EVENT_TYPE = "PRESENTER_CONTROL_CHANGED";

    static PresenterControlChangedMessage of(UUID sessionId, Instant occurredAt) {
        return new PresenterControlChangedMessage(EVENT_TYPE, sessionId, occurredAt);
    }

    boolean isRelayable() {
        return (type == null || EVENT_TYPE.equals(type))
            && sessionId != null
            && occurredAt != null;
    }
}
