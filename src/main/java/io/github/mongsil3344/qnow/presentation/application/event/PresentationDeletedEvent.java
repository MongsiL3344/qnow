package io.github.mongsil3344.qnow.presentation.application.event;

import java.util.UUID;

public record PresentationDeletedEvent(
    UUID sessionId,
    UUID presentationId
) {}
