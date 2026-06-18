package io.github.mongsil3344.qnow.bff.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrganizationDetailResult(
    UUID id,
    String name,
    String detail,
    long memberCount,
    List<SessionResult> sessions
) {

    public record SessionResult(
        UUID id,
        String title,
        String creatorName,
        Instant startAt,
        Instant endAt,
        long participantCount
    ) {
    }
}
