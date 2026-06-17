package io.github.mongsil3344.qnow.organizationdetail.application;

import io.github.mongsil3344.qnow.organization.api.OrganizationInfo;
import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.organizationdetail.application.dto.OrganizationDetailResult;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.session.api.SessionSummary;
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
public class GetOrganizationDetailService {

    private static final String UNKNOWN_USER_NAME = "알 수 없는 사용자";

    private final OrganizationQueryApi organizationQueryApi;
    private final SessionQueryApi sessionQueryApi;
    private final UserQueryApi userQueryApi;

    @Transactional(readOnly = true)
    public OrganizationDetailResult getOrganizationDetail(UUID organizationId, UUID userId) {
        OrganizationInfo organizationInfo = organizationQueryApi.getOrganizationInfo(organizationId, userId);
        List<SessionSummary> sessions = sessionQueryApi.findSessionSummariesByOrganizationId(organizationId);

        if (sessions.isEmpty()) {
            return new OrganizationDetailResult(
                organizationInfo.id(),
                organizationInfo.name(),
                organizationInfo.detail(),
                organizationInfo.memberCount(),
                List.of()
            );
        }

        Map<UUID, String> creatorNames = getCreatorNames(sessions);

        List<OrganizationDetailResult.SessionResult> sessionResults = sessions.stream()
            .map(session -> new OrganizationDetailResult.SessionResult(
                session.id(),
                session.title(),
                creatorNames.getOrDefault(session.creatorId(), UNKNOWN_USER_NAME),
                session.startAt(),
                session.endAt(),
                session.participantCount()
            ))
            .toList();

        return new OrganizationDetailResult(
            organizationInfo.id(),
            organizationInfo.name(),
            organizationInfo.detail(),
            organizationInfo.memberCount(),
            sessionResults
        );
    }

    private Map<UUID, String> getCreatorNames(List<SessionSummary> sessions) {
        Set<UUID> creatorIds = sessions.stream()
            .map(SessionSummary::creatorId)
            .collect(Collectors.toSet());

        return userQueryApi.findNicknamesByIds(creatorIds);
    }
}
