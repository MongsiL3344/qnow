package io.github.mongsil3344.qnow.presentation.application;

import io.github.mongsil3344.qnow.presentation.application.exception.PresentationSessionNotFoundException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresenterViewParticipantRequiredException;
import io.github.mongsil3344.qnow.session.api.SessionAccessApi;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.session.api.SessionStatusApi;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class PresenterViewAccessValidator {

    private final SessionQueryApi sessionQueryApi;
    private final SessionStatusApi sessionStatusApi;
    private final SessionAccessApi sessionAccessApi;
    private final PresenterControlStore presenterControlStore;

    // 세션 유효성 검증 + Host 여부 반환 메서드
    public boolean isSessionCreator(
        UUID organizationId,
        UUID sessionId,
        UUID userId
    ) {
        return isSessionCreator(
            organizationId,
            sessionId,
            new SessionActor.Member(userId)
        );
    }

    public boolean isSessionCreator(
        UUID organizationId,
        UUID sessionId,
        SessionActor actor
    ) {
        validateParticipant(organizationId, sessionId, actor);
        return isCreator(sessionId, actor);
    }

    public PresenterControlStatus getControlStatus(
        UUID organizationId,
        UUID sessionId,
        SessionActor actor
    ) {
        UUID participantId = validateParticipant(organizationId, sessionId, actor);
        boolean creator = isCreator(sessionId, actor);
        Instant controlExpiresAt = null;
        if (!creator) {
            controlExpiresAt = presenterControlStore.getExpiry(sessionId, participantId).orElse(null);
        }

        return new PresenterControlStatus(
            creator,
            creator || controlExpiresAt != null,
            controlExpiresAt,
            participantId
        );
    }

    private UUID validateParticipant(
        UUID organizationId,
        UUID sessionId,
        SessionActor actor
    ) {
        if (!sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)) {
            throw new PresentationSessionNotFoundException();
        }

        sessionStatusApi.requireNotEnded(sessionId);

        return sessionQueryApi.findActiveParticipantId(sessionId, actor)
            .orElseThrow(PresenterViewParticipantRequiredException::new);
    }

    private boolean isCreator(UUID sessionId, SessionActor actor) {
        return actor instanceof SessionActor.Member member
            && sessionAccessApi.isSessionCreator(sessionId, member.userId());
    }

    public record PresenterControlStatus(
        boolean creator,
        boolean canControl,
        Instant controlExpiresAt,
        UUID participantId
    ) {
    }
}
