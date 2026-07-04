package io.github.mongsil3344.qnow.session.application;

import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.session.application.exception.NotOrganizationMemberException;
import io.github.mongsil3344.qnow.session.application.exception.SessionNotFoundException;
import io.github.mongsil3344.qnow.session.infrastructure.repo.ParticipantRepository;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionRepository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class ExitSessionService {

    private final SessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final OrganizationQueryApi organizationQueryApi;

    @Transactional
    public void exitSession(UUID organizationId, UUID sessionId, UUID userId) {
        boolean existsSession = sessionRepository.existsByIdAndOrganizationIdAndDeletedAtIsNull(
            sessionId,
            organizationId
        );
        if (!existsSession) {
            throw new SessionNotFoundException();
        }

        boolean existsUserInOrganization = organizationQueryApi.existsUserInOrganization(userId, organizationId);
        if (!existsUserInOrganization) {
            throw new NotOrganizationMemberException();
        }

        participantRepository.findByUserIdAndSessionIdAndDeletedAtIsNull(userId, sessionId)
            .ifPresent(participant -> participant.exit());
    }
}
