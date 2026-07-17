package io.github.mongsil3344.qnow.bff.infrastructure.web.dto;

import io.github.mongsil3344.qnow.bff.application.dto.SessionScreenResult;
import java.time.Instant;
import java.util.UUID;

public record SessionScreenResponse(
    UUID id,
    String title,
    String creatorName,
    Instant startAt,
    Instant endAt,
    long participantCount,
    boolean canUpload,
    boolean canEnd
) {

    public static SessionScreenResponse from(SessionScreenResult result) {
        return new SessionScreenResponse(
            result.id(),
            result.title(),
            result.creatorName(),
            result.startAt(),
            result.endAt(),
            result.participantCount(),
            result.canUpload(),
            result.canEnd()
        );
    }
}
