package io.github.mongsil3344.qnow.presentation.application;

import io.github.mongsil3344.qnow.presentation.application.exception.PresentationSessionNotFoundException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresenterViewParticipantRequiredException;
import io.github.mongsil3344.qnow.session.api.SessionAccessApi;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.session.api.SessionStatusApi;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class PresenterViewAccessValidator {

    private final SessionQueryApi sessionQueryApi;
    private final SessionStatusApi sessionStatusApi;
    private final SessionAccessApi sessionAccessApi;

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
        if (!sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)) {
            throw new PresentationSessionNotFoundException();
        }

        sessionStatusApi.requireNotEnded(sessionId);

        if (!sessionQueryApi.isActiveParticipant(sessionId, actor)) {
            throw new PresenterViewParticipantRequiredException();
        }

        return actor instanceof SessionActor.Member member
            && sessionAccessApi.isSessionCreator(sessionId, member.userId());
    }
}
