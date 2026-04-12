package io.github.mongsil3344.qnow.session.application;

import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.session.domain.Participant;
import io.github.mongsil3344.qnow.session.application.exception.NotOrganizationMemberException;
import io.github.mongsil3344.qnow.session.application.exception.OrganizationAdminRequiredException;
import io.github.mongsil3344.qnow.session.domain.Session;
import io.github.mongsil3344.qnow.session.infrastructure.repo.ParticipantRepository;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionRepository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class CreateSessionService {

    private final SessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final OrganizationQueryApi organizationQueryApi;

    @Transactional
    public void createSession(UUID organizationId, UUID creatorId, String title) {

        boolean existUserInGroup = organizationQueryApi.existsUserInOrganization(creatorId, organizationId);
        if (!existUserInGroup) {
            throw new NotOrganizationMemberException();
        }

        boolean isAdminInGroup = organizationQueryApi.isAdminInOrganization(creatorId, organizationId);
        if (!isAdminInGroup) {
            throw new OrganizationAdminRequiredException();
        }

        Session newSession = Session.builder()
            .organizationId(organizationId)
            .creatorId(creatorId)
            .title(title)
            .build();

        Session savedSession = sessionRepository.save(newSession);

        Participant creatorParticipant = Participant.builder()
            .userId(creatorId)
            .session(savedSession)
            .build();

        participantRepository.save(creatorParticipant);
    }
}
