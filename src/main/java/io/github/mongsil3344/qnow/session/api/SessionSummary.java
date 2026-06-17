package io.github.mongsil3344.qnow.session.api;

import java.time.Instant;
import java.util.UUID;

public record SessionSummary(
    UUID id,
    String title,
    UUID creatorId,
    Instant startAt,
    Instant endAt,
    long participantCount
) {
}
