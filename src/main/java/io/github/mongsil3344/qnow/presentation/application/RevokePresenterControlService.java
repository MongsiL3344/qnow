package io.github.mongsil3344.qnow.presentation.application;

import io.github.mongsil3344.qnow.presentation.application.exception.PresenterViewControlForbiddenException;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevokePresenterControlService {

    private final PresenterViewAccessValidator accessValidator;
    private final PresenterControlStore controlStore;

    public RevokePresenterControlService(
        PresenterViewAccessValidator accessValidator,
        PresenterControlStore controlStore
    ) {
        this.accessValidator = accessValidator;
        this.controlStore = controlStore;
    }

    @Transactional
    public void revoke(
        UUID organizationId,
        UUID sessionId,
        SessionActor actor,
        UUID participantId
    ) {
        PresenterViewAccessValidator.PresenterControlStatus status = accessValidator.getControlStatus(
            organizationId,
            sessionId,
            actor
        );
        if (!status.creator() && !participantId.equals(status.participantId())) {
            throw new PresenterViewControlForbiddenException();
        }

        controlStore.revoke(sessionId, participantId);
    }
}
