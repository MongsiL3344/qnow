package io.github.mongsil3344.qnow.session.application.dto;

import java.util.UUID;

public record SessionParticipateCodeResult(
    UUID sessionId,
    String code
) {
}
