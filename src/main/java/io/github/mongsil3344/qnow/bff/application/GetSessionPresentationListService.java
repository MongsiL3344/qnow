package io.github.mongsil3344.qnow.bff.application;

import io.github.mongsil3344.qnow.bff.application.dto.SessionPresentationListResult;
import io.github.mongsil3344.qnow.bff.application.exception.SessionPresentationNotFoundException;
import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.presentation.api.PresentationQueryApi;
import io.github.mongsil3344.qnow.presentation.api.SessionPresentationSummary;
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
        organizationQueryApi.getOrganizationInfo(organizationId, userId);
        validateSessionInOrganization(sessionId, organizationId);

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
                    presentation.thumbnailUrl()
                ))
                .toList()
        );
    }

    private void validateSessionInOrganization(UUID sessionId, UUID organizationId) {
        if (!sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)) {
            throw new SessionPresentationNotFoundException();
        }
    }

    private Map<UUID, String> getPresenterNames(List<SessionPresentationSummary> presentations) {
        Set<UUID> presenterIds = presentations.stream()
            .map(SessionPresentationSummary::presenterId)
            .collect(Collectors.toSet());

        return userQueryApi.findNicknamesByIds(presenterIds);
    }
}
