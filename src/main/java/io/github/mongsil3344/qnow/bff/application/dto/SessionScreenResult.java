package io.github.mongsil3344.qnow.bff.application.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionScreenResult(
    UUID id,
    String title,
    String creatorName,
    Instant startAt,
    Instant endAt,
    long participantCount,
    boolean canUpload,
    boolean canEnd
) {
}
