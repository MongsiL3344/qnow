package io.github.mongsil3344.qnow.bff.infrastructure.web.dto;

import io.github.mongsil3344.qnow.bff.application.dto.SessionPresentationListResult;
import java.util.List;
import java.util.UUID;

public record SessionPresentationListResponse(
    List<PresentationResponse> presentations
) {

    public static SessionPresentationListResponse from(SessionPresentationListResult result) {
        return new SessionPresentationListResponse(
            result.presentations().stream()
                .map(PresentationResponse::from)
                .toList()
        );
    }

    public record PresentationResponse(
        UUID presentationId,
        String title,
        String presenter,
        String thumbnailUrl,
        boolean canDelete
    ) {

        private static PresentationResponse from(SessionPresentationListResult.PresentationResult result) {
            return new PresentationResponse(
                result.presentationId(),
                result.title(),
                result.presenter(),
                result.thumbnailUrl(),
                result.canDelete()
            );
        }
    }
}
