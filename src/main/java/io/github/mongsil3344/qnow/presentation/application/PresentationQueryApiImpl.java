package io.github.mongsil3344.qnow.presentation.application;

import io.github.mongsil3344.qnow.presentation.api.PresentationQueryApi;
import io.github.mongsil3344.qnow.presentation.api.SessionPresentationSummary;
import io.github.mongsil3344.qnow.presentation.domain.UploadStatus;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Component
public class PresentationQueryApiImpl implements PresentationQueryApi {

    private final PresentationRepository presentationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SessionPresentationSummary> findUploadedPresentationSummariesBySessionId(UUID sessionId) {
        return presentationRepository
            .findAllBySessionIdAndUploadStatusAndDeletedAtIsNullOrderByCreatedAtDesc(sessionId, UploadStatus.UPLOADED)
            .stream()
            .map(presentation -> new SessionPresentationSummary(
                presentation.getTitle(),
                presentation.getPresenterId()
            ))
            .toList();
    }
}
