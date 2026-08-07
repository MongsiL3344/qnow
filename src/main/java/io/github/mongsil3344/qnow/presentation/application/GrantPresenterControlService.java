package io.github.mongsil3344.qnow.presentation.application;

import io.github.mongsil3344.qnow.presentation.application.exception.PresenterControlTargetInvalidException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresenterViewControlForbiddenException;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrantPresenterControlService {

    private final PresenterViewAccessValidator accessValidator;
    private final SessionQueryApi sessionQueryApi;
    private final PresenterControlStore controlStore;
    private final Duration controlTtl;

    public GrantPresenterControlService(
        PresenterViewAccessValidator accessValidator,
        SessionQueryApi sessionQueryApi,
        PresenterControlStore controlStore,
        @Value("${qnow.presenter-view.control-ttl:10m}") Duration controlTtl
    ) {
        this.accessValidator = accessValidator;
        this.sessionQueryApi = sessionQueryApi;
        this.controlStore = controlStore;
        this.controlTtl = controlTtl;
    }

    @Transactional
    public void grant(
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
        if (!status.creator()) {
            throw new PresenterViewControlForbiddenException();
        }

        if (!sessionQueryApi.isActiveParticipant(sessionId, participantId)) {
            throw new PresenterControlTargetInvalidException();
        }

        controlStore.grant(sessionId, participantId, Instant.now().plus(controlTtl));
    }
}
