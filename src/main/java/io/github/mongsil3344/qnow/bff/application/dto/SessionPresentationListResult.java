package io.github.mongsil3344.qnow.bff.application.dto;

import java.util.List;

public record SessionPresentationListResult(
    List<PresentationResult> presentations
) {

    public record PresentationResult(
        String title,
        String presenter
    ) {
    }
}
