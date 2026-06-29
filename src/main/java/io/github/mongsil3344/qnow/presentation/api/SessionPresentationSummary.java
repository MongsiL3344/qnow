package io.github.mongsil3344.qnow.presentation.api;

import java.util.UUID;

public record SessionPresentationSummary(
    String title,
    UUID presenterId
) {
}
