package io.github.mongsil3344.qnow.presentation.api;

import java.util.UUID;

public record SessionPresentationSummary(
    UUID presentationId,
    String title,
    UUID presenterId,
    String thumbnailUrl
) {
}
