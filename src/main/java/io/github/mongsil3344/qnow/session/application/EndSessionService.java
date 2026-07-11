package io.github.mongsil3344.qnow.session.application;

import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.session.application.exception.OrganizationAdminRequiredException;
import io.github.mongsil3344.qnow.session.application.exception.SessionNotFoundException;
import io.github.mongsil3344.qnow.session.domain.Session;
import io.github.mongsil3344.qnow.session.infrastructure.repo.ParticipantRepository;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class EndSessionService {

    private final SessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final OrganizationQueryApi organizationQueryApi;

    @Transactional
    public void endSession(UUID organizationId, UUID sessionId, UUID userId) {
        Session session = sessionRepository.findByIdAndOrganizationIdForLifecycleUpdate(sessionId, organizationId)
            .orElseThrow(SessionNotFoundException::new);

        if (!organizationQueryApi.isAdminInOrganization(userId, organizationId)) {
            throw new OrganizationAdminRequiredException();
        }

        if (session.isEnded()) {
            return;
        }

        Instant endedAt = Instant.now();
        session.end(endedAt);
        participantRepository.exitAllActiveParticipants(sessionId, endedAt);
    }
}
