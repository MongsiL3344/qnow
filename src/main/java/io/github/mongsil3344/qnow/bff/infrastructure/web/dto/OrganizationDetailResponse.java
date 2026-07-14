package io.github.mongsil3344.qnow.bff.infrastructure.web.dto;

import io.github.mongsil3344.qnow.bff.application.dto.OrganizationDetailResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrganizationDetailResponse(
    UUID id,
    String name,
    String detail,
    long memberCount,
    boolean isAdmin,
    List<SessionResponse> sessions
) {

    public static OrganizationDetailResponse from(OrganizationDetailResult result) {
        return new OrganizationDetailResponse(
            result.id(),
            result.name(),
            result.detail(),
            result.memberCount(),
            result.isAdmin(),
            result.sessions().stream()
                .map(SessionResponse::from)
                .toList()
        );
    }

    public record SessionResponse(
        UUID id,
        String title,
        String creatorName,
        Instant startAt,
        Instant endAt,
        long participantCount,
        boolean canEnd
    ) {

        private static SessionResponse from(OrganizationDetailResult.SessionResult result) {
            return new SessionResponse(
                result.id(),
                result.title(),
                result.creatorName(),
                result.startAt(),
                result.endAt(),
                result.participantCount(),
                result.canEnd()
            );
        }
    }
}
