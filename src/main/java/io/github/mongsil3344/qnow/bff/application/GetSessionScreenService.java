package io.github.mongsil3344.qnow.bff.application;

import io.github.mongsil3344.qnow.bff.application.dto.SessionScreenResult;
import io.github.mongsil3344.qnow.bff.application.exception.SessionScreenNotFoundException;
import io.github.mongsil3344.qnow.bff.application.exception.SessionScreenOrganizationMemberRequiredException;
import io.github.mongsil3344.qnow.bff.application.exception.SessionScreenParticipantRequiredException;
import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.session.api.SessionSummary;
import io.github.mongsil3344.qnow.user.api.UserQueryApi;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class GetSessionScreenService {

    private static final String UNKNOWN_USER_NAME = "알 수 없는 사용자";

    private final OrganizationQueryApi organizationQueryApi;
    private final SessionQueryApi sessionQueryApi;
    private final UserQueryApi userQueryApi;

    @Transactional(readOnly = true)
    public SessionScreenResult getSessionScreen(
        UUID organizationId,
        UUID sessionId,
        SessionActor actor
    ) {
        SessionSummary session = sessionQueryApi.findSessionSummary(sessionId, organizationId)
            .orElseThrow(SessionScreenNotFoundException::new);
        ScreenCapabilities capabilities = getCapabilities(organizationId, session, actor);
        String creatorName = userQueryApi.findNicknamesByIds(Set.of(session.creatorId()))
            .getOrDefault(session.creatorId(), UNKNOWN_USER_NAME);

        return new SessionScreenResult(
            session.id(),
            session.title(),
            creatorName,
            session.startAt(),
            session.endAt(),
            session.participantCount(),
            capabilities.canUpload(),
            capabilities.canEnd()
        );
    }

    private ScreenCapabilities getCapabilities(
        UUID organizationId,
        SessionSummary session,
        SessionActor actor
    ) {
        return switch (actor) {
            case SessionActor.Member member -> getMemberCapabilities(
                organizationId,
                session,
                member.userId()
            );
            case SessionActor.Guest ignored -> getGuestCapabilities(session.id(), actor);
        };
    }

    private ScreenCapabilities getMemberCapabilities(
        UUID organizationId,
        SessionSummary session,
        UUID userId
    ) {
        if (!organizationQueryApi.existsUserInOrganization(userId, organizationId)) {
            throw new SessionScreenOrganizationMemberRequiredException();
        }

        boolean active = session.endAt() == null;
        boolean canEnd = active && organizationQueryApi.isAdminInOrganization(userId, organizationId);
        return new ScreenCapabilities(active, canEnd);
    }

    private ScreenCapabilities getGuestCapabilities(UUID sessionId, SessionActor actor) {
        if (sessionQueryApi.findActiveParticipantId(sessionId, actor).isEmpty()) {
            throw new SessionScreenParticipantRequiredException();
        }

        return new ScreenCapabilities(false, false);
    }

    private record ScreenCapabilities(
        boolean canUpload,
        boolean canEnd
    ) {
    }
}
