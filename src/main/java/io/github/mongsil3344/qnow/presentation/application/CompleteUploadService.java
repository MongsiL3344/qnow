package io.github.mongsil3344.qnow.presentation.application;

import io.github.mongsil3344.qnow.presentation.application.exception.InvalidUploadObjectKeyException;
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class CompleteUploadService {

    private final PresentationRepository presentationRepository;

    @Transactional
    public void completeUpload(UUID organizationId, UUID sessionId, String objectKey) {
        String expectedPrefix = objectKeyPrefix(organizationId, sessionId);
        if (!objectKey.startsWith(expectedPrefix)) {
            throw new InvalidUploadObjectKeyException();
        }

        Presentation presentation = presentationRepository.findByS3KeyAndSessionIdAndDeletedAtIsNull(objectKey, sessionId)
                .orElseThrow(InvalidUploadObjectKeyException::new);
        presentation.setStatusUploaded();
    }

    private String objectKeyPrefix(UUID organizationId, UUID sessionId) {
        return "presentations/%s/%s/".formatted(organizationId, sessionId);
    }
}
