package io.github.mongsil3344.qnow.session.application;

import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.session.application.exception.NotOrganizationMemberException;
import io.github.mongsil3344.qnow.session.application.exception.SessionNotFoundException;
import io.github.mongsil3344.qnow.session.domain.Session;
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
        exitSession(organizationId, sessionId, new SessionActor.Member(userId));
    }

    @Transactional
    public void exitSession(
        UUID organizationId,
        UUID sessionId,
        SessionActor actor
    ) {
        Session session = sessionRepository.findByIdAndOrganizationIdForLifecycleRead(sessionId, organizationId)
            .orElseThrow(SessionNotFoundException::new);

        if (session.isEnded()) {
            return;
        }

        switch (actor) {
            case SessionActor.Member member -> exitMember(
                organizationId,
                sessionId,
                member.userId()
            );
            case SessionActor.Guest guest -> exitGuest(sessionId, guest);
        }
    }

    private void exitMember(UUID organizationId, UUID sessionId, UUID userId) {
        boolean existsUserInOrganization = organizationQueryApi.existsUserInOrganization(userId, organizationId);
        if (!existsUserInOrganization) {
            throw new NotOrganizationMemberException();
        }

        participantRepository.findByUserIdAndSessionIdAndDeletedAtIsNull(userId, sessionId)
            .ifPresent(participant -> participant.exit());
    }

    private void exitGuest(UUID sessionId, SessionActor.Guest guest) {
        if (!sessionId.equals(guest.sessionId())) {
            return;
        }

        participantRepository.findByIdAndSessionIdAndUserIdIsNullAndDeletedAtIsNull(
            guest.participantId(),
            sessionId
        ).ifPresent(participant -> participant.exit());
    }
}
