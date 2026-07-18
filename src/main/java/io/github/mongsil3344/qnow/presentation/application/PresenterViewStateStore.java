package io.github.mongsil3344.qnow.presentation.application;

import io.github.mongsil3344.qnow.presentation.domain.PresenterViewClearReason;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewSnapshot;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PresenterViewStateStore {

    // Redis 스냅샷 조회
    PresenterViewSnapshot get(UUID sessionId);

    // Redis 스냅샷 업데이트, Pub/Sub 이벤트 발행
    PresenterViewSnapshot update(
        UUID sessionId,
        UUID presentationId,
        int pageNumber,
        Instant updatedAt
    );

    Optional<PresenterViewSnapshot> clearPresentation(
        UUID sessionId,
        UUID presentationId,
        Instant updatedAt,
        PresenterViewClearReason reason
    );

    void clearSession(UUID sessionId, Instant updatedAt, PresenterViewClearReason reason);
}
