package io.github.mongsil3344.qnow.session.application;

import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.session.api.ParticipantExitedEvent;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.session.application.exception.NotOrganizationMemberException;
import io.github.mongsil3344.qnow.session.application.exception.SessionNotFoundException;
import io.github.mongsil3344.qnow.session.domain.Session;
import io.github.mongsil3344.qnow.session.infrastructure.repo.ParticipantRepository;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionRepository;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class ExitSessionService {

    private final SessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final OrganizationQueryApi organizationQueryApi;
    private final ApplicationEventPublisher eventPublisher;

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

        Optional<UUID> exitedParticipantId = switch (actor) {
            case SessionActor.Member member -> exitMember(
                organizationId,
                sessionId,
                member.userId()
            );
            case SessionActor.Guest guest -> exitGuest(sessionId, guest);
        };

        exitedParticipantId.ifPresent(participantId -> eventPublisher.publishEvent(
            new ParticipantExitedEvent(sessionId, participantId)
        ));
    }

    private Optional<UUID> exitMember(UUID organizationId, UUID sessionId, UUID userId) {
        boolean existsUserInOrganization = organizationQueryApi.existsUserInOrganization(userId, organizationId);
        if (!existsUserInOrganization) {
            throw new NotOrganizationMemberException();
        }

        return participantRepository.findByUserIdAndSessionIdAndDeletedAtIsNull(userId, sessionId)
            .map(participant -> {
                participant.exit();
                return participant.getId();
            });
    }

    private Optional<UUID> exitGuest(UUID sessionId, SessionActor.Guest guest) {
        if (!sessionId.equals(guest.sessionId())) {
            return Optional.empty();
        }

        return participantRepository.findByIdAndSessionIdAndUserIdIsNullAndDeletedAtIsNull(
            guest.participantId(),
            sessionId
        ).map(participant -> {
            participant.exit();
            return participant.getId();
        });
    }
}
