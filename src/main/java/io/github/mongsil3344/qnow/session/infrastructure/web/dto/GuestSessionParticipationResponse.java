package io.github.mongsil3344.qnow.session.infrastructure.web.dto;

import io.github.mongsil3344.qnow.session.application.dto.JoinGuestSessionResult;
import java.util.UUID;

public record GuestSessionParticipationResponse(
    UUID participantId,
    UUID sessionId,
    UUID organizationId,
    String nickname
) {

    public static GuestSessionParticipationResponse from(JoinGuestSessionResult result) {
        return new GuestSessionParticipationResponse(
            result.participantId(),
            result.sessionId(),
            result.organizationId(),
            result.nickname()
        );
    }
}
