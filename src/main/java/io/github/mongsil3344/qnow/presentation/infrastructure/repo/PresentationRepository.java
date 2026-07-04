package io.github.mongsil3344.qnow.presentation.infrastructure.repo;

import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.domain.UploadStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresentationRepository extends JpaRepository<Presentation, UUID> {

    Optional<Presentation> findByS3KeyAndSessionIdAndDeletedAtIsNull(String s3Key, UUID sessionId);

    Optional<Presentation> findByIdAndSessionIdAndDeletedAtIsNull(UUID id, UUID sessionId);

    Optional<Presentation> findByIdAndSessionIdAndUploadStatusAndDeletedAtIsNull(
        UUID id,
        UUID sessionId,
        UploadStatus uploadStatus
    );

    List<Presentation> findAllBySessionIdAndUploadStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
        UUID sessionId,
        UploadStatus uploadStatus
    );
}
