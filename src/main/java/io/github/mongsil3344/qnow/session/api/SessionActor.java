package io.github.mongsil3344.qnow.session.api;

import java.util.Objects;
import java.util.UUID;

public sealed interface SessionActor {

    record Member(UUID userId) implements SessionActor {

        public Member {
            Objects.requireNonNull(userId);
        }
    }

    record Guest(UUID participantId, UUID sessionId) implements SessionActor {

        public Guest {
            Objects.requireNonNull(participantId);
            Objects.requireNonNull(sessionId);
        }
    }
}
