package io.github.mongsil3344.qnow.presentation.infrastructure.repo;

import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresentationRepository extends JpaRepository<Presentation, UUID> {

    Optional<Presentation> findByS3KeyAndSessionIdAndDeletedAtIsNull(String s3Key, UUID sessionId);
}
