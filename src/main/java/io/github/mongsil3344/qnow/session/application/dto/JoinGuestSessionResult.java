package io.github.mongsil3344.qnow.session.application.dto;

import java.util.UUID;

public record JoinGuestSessionResult(
    UUID participantId,
    UUID sessionId,
    UUID organizationId,
    String nickname
) {
}
