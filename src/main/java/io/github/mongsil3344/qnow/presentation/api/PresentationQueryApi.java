package io.github.mongsil3344.qnow.presentation.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PresentationQueryApi {

    List<SessionPresentationSummary> findUploadedPresentationSummariesBySessionId(UUID sessionId);

    Optional<UploadedPresentationInfo> findUploadedPresentationById(UUID presentationId);
}
