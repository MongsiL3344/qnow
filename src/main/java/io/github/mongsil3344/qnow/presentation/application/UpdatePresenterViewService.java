package io.github.mongsil3344.qnow.presentation.application;

import io.github.mongsil3344.qnow.presentation.application.dto.PresenterViewResult;
import io.github.mongsil3344.qnow.presentation.application.exception.InvalidPresenterViewPageException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationNotFoundException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresenterViewControlForbiddenException;
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.domain.UploadStatus;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdatePresenterViewService {

    private final PresenterViewAccessValidator accessValidator;
    private final PresenterViewStateStore stateStore;
    private final PresentationRepository presentationRepository;

    public UpdatePresenterViewService(
        PresenterViewAccessValidator accessValidator,
        PresenterViewStateStore stateStore,
        PresentationRepository presentationRepository
    ) {
        this.accessValidator = accessValidator;
        this.stateStore = stateStore;
        this.presentationRepository = presentationRepository;
    }

    @Transactional
    public PresenterViewResult updatePresenterView(
        UUID organizationId,
        UUID sessionId,
        SessionActor actor,
        UUID presentationId,
        int pageNumber
    ) {
        PresenterViewAccessValidator.PresenterControlStatus controlStatus = accessValidator.getControlStatus(
            organizationId,
            sessionId,
            actor
        );
        if (!controlStatus.canControl()) {
            throw new PresenterViewControlForbiddenException();
        }

        // 발표자료 조회
        Presentation presentation = presentationRepository
            .findByIdAndSessionIdAndUploadStatusAndDeletedAtIsNull(
                presentationId,
                sessionId,
                UploadStatus.UPLOADED
            ).orElseThrow(PresentationNotFoundException::new);
        if (pageNumber < 1 || pageNumber > presentation.getPageCount()) {
            throw new InvalidPresenterViewPageException();
        }

        // 스냅샷 저장하고 Pub/Sub 이벤트 발행
        var snapshot = stateStore.update(
            sessionId,
            presentationId,
            pageNumber,
            Instant.now()
        );

        return PresenterViewResult.from(
            true,
            controlStatus.controlExpiresAt(),
            snapshot
        );
    }
}
