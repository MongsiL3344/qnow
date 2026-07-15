package io.github.mongsil3344.qnow.bff.application;

import io.github.mongsil3344.qnow.bff.application.dto.SessionPresentationListResult;
import io.github.mongsil3344.qnow.bff.application.exception.SessionPresentationNotFoundException;
import io.github.mongsil3344.qnow.bff.application.exception.SessionPresentationParticipantRequiredException;
import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.presentation.api.PresentationQueryApi;
import io.github.mongsil3344.qnow.presentation.api.SessionPresentationSummary;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.user.api.UserQueryApi;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class GetSessionPresentationListService {

    private static final String UNKNOWN_USER_NAME = "알 수 없는 사용자";

    private final OrganizationQueryApi organizationQueryApi;
    private final SessionQueryApi sessionQueryApi;
    private final PresentationQueryApi presentationQueryApi;
    private final UserQueryApi userQueryApi;

    @Transactional(readOnly = true)
    public SessionPresentationListResult getSessionPresentations(
        UUID organizationId,
        UUID sessionId,
        UUID userId
    ) {
        return getSessionPresentations(
            organizationId,
            sessionId,
            new SessionActor.Member(userId)
        );
    }

    @Transactional(readOnly = true)
    public SessionPresentationListResult getSessionPresentations(
        UUID organizationId,
        UUID sessionId,
        SessionActor actor
    ) {
        validateSessionInOrganization(sessionId, organizationId);
        validateAccess(sessionId, organizationId, actor);

        List<SessionPresentationSummary> presentations =
            presentationQueryApi.findUploadedPresentationSummariesBySessionId(sessionId);

        if (presentations.isEmpty()) {
            return new SessionPresentationListResult(List.of());
        }

        Map<UUID, String> presenterNames = getPresenterNames(presentations);

        return new SessionPresentationListResult(
            presentations.stream()
                .map(presentation -> new SessionPresentationListResult.PresentationResult(
                    presentation.presentationId(),
                    presentation.title(),
                    presenterNames.getOrDefault(presentation.presenterId(), UNKNOWN_USER_NAME),
                    presentation.thumbnailUrl(),
                    canDeletePresentation(actor, presentation.presenterId())
                ))
                .toList()
        );
    }

    private void validateSessionInOrganization(UUID sessionId, UUID organizationId) {
        if (!sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)) {
            throw new SessionPresentationNotFoundException();
        }
    }

    private void validateAccess(
        UUID sessionId,
        UUID organizationId,
        SessionActor actor
    ) {
        switch (actor) {
            case SessionActor.Member member ->
                organizationQueryApi.getOrganizationInfo(organizationId, member.userId());
            case SessionActor.Guest ignored -> {
                if (sessionQueryApi.findActiveParticipantId(sessionId, actor).isEmpty()) {
                    throw new SessionPresentationParticipantRequiredException();
                }
            }
        }
    }

    private boolean canDeletePresentation(SessionActor actor, UUID presenterId) {
        return actor instanceof SessionActor.Member member
            && presenterId.equals(member.userId());
    }

    private Map<UUID, String> getPresenterNames(List<SessionPresentationSummary> presentations) {
        Set<UUID> presenterIds = presentations.stream()
            .map(SessionPresentationSummary::presenterId)
            .collect(Collectors.toSet());

        return userQueryApi.findNicknamesByIds(presenterIds);
    }
}
