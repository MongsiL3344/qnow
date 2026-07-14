package io.github.mongsil3344.qnow.presentation.application;

import io.github.mongsil3344.qnow.presentation.application.dto.PresenterViewResult;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewClearReason;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewSnapshot;
import io.github.mongsil3344.qnow.presentation.domain.UploadStatus;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class GetPresenterViewService {

    private final PresenterViewAccessValidator accessValidator;
    private final PresenterViewStateStore stateStore;
    private final PresentationRepository presentationRepository;

    @Transactional
    public PresenterViewResult getPresenterView(UUID organizationId, UUID sessionId, UUID userId) {
        // 세션 Host만 컨트롤 가능 (Host면 true 반환)
        boolean canControl = accessValidator.isSessionCreator(organizationId, sessionId, userId);

        // 해당 세션의 Redis 스냅샷 객체를 조회
        PresenterViewSnapshot snapshot = stateStore.get(sessionId);

        // 스냅샷 객체의 발표가 없으면 스냅샷에 담긴 정보를 비움
        if (snapshot.hasView() && presentationRepository
            .findByIdAndSessionIdAndUploadStatusAndDeletedAtIsNull(
                snapshot.presentationId(),
                snapshot.sessionId(),
                UploadStatus.UPLOADED
            ).isEmpty()) {
            snapshot = stateStore.clearPresentation(
                sessionId,
                snapshot.presentationId(),
                Instant.now(),
                PresenterViewClearReason.PRESENTATION_DELETED // 스냅샷을 비웠다면 비워진 스냅샷을 그대로 사용
            ).orElseGet(() -> stateStore.get(sessionId));     // 중간에 발표정보가 바뀌어서 스냅샷을 비우지 못했을때 다시 조회 수행
        }

        // 스냅샷 객체 반환
        return PresenterViewResult.from(canControl, snapshot);
    }
}
