package io.github.mongsil3344.qnow.bff.application.dto;

import java.util.List;
import java.util.UUID;

public record SessionPresentationListResult(
    List<PresentationResult> presentations
) {

    public record PresentationResult(
        UUID presentationId,
        String title,
        String presenter,
        String thumbnailUrl
    ) {
    }
}
