package io.github.mongsil3344.qnow.session.infrastructure.web.dto;

import io.github.mongsil3344.qnow.session.application.dto.SessionParticipateCodeResult;
import java.util.UUID;

public record SessionParticipateCodeResponse(
    UUID sessionId,
    String code
) {

    public static SessionParticipateCodeResponse from(SessionParticipateCodeResult result) {
        return new SessionParticipateCodeResponse(result.sessionId(), result.code());
    }
}
