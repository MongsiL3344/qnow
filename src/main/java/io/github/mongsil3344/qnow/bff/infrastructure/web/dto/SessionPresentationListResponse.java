package io.github.mongsil3344.qnow.bff.infrastructure.web.dto;

import io.github.mongsil3344.qnow.bff.application.dto.SessionPresentationListResult;
import java.util.List;

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
        String title,
        String presenter
    ) {

        private static PresentationResponse from(SessionPresentationListResult.PresentationResult result) {
            return new PresentationResponse(
                result.title(),
                result.presenter()
            );
        }
    }
}
