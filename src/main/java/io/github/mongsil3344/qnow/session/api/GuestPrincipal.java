package io.github.mongsil3344.qnow.session.api;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;

public record GuestPrincipal(
    UUID participantId,
    UUID sessionId
) implements Principal, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public GuestPrincipal {
        Objects.requireNonNull(participantId);
        Objects.requireNonNull(sessionId);
    }

    @Override
    public String getName() {
        return participantId.toString();
    }
}
