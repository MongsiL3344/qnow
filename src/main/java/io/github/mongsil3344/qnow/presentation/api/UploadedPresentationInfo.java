package io.github.mongsil3344.qnow.presentation.api;

import java.util.UUID;

public record UploadedPresentationInfo(
    UUID presentationId,
    UUID sessionId,
    int pageCount
) {
}
