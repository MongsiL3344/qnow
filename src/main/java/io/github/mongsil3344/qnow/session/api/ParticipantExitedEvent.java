package io.github.mongsil3344.qnow.session.api;

import java.util.Objects;
import java.util.UUID;

public record ParticipantExitedEvent(
    UUID sessionId,
    UUID participantId
) {

    public ParticipantExitedEvent {
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(participantId);
    }
}
