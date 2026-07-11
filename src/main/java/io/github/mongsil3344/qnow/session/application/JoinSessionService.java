package io.github.mongsil3344.qnow.session.application;

import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.session.api.SessionEndedException;
import io.github.mongsil3344.qnow.session.application.exception.AlreadySessionParticipantException;
import io.github.mongsil3344.qnow.session.application.exception.NotOrganizationMemberException;
import io.github.mongsil3344.qnow.session.application.exception.SessionNotFoundException;
import io.github.mongsil3344.qnow.session.domain.Participant;
import io.github.mongsil3344.qnow.session.domain.Session;
import io.github.mongsil3344.qnow.session.infrastructure.repo.ParticipantRepository;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionRepository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class JoinSessionService {

    private final SessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final OrganizationQueryApi organizationQueryApi;

    @Transactional
    public void joinSession(UUID organizationId, UUID sessionId, UUID userId) {
        Session session = sessionRepository.findByIdAndOrganizationIdForLifecycleRead(sessionId, organizationId)
            .orElseThrow(SessionNotFoundException::new);

        if (session.isEnded()) {
            throw new SessionEndedException();
        }

        // 만약 조직에 존재하지 않는 유저면 예외발생
        boolean existsUserInOrganization = organizationQueryApi.existsUserInOrganization(userId, organizationId);
        if (!existsUserInOrganization) {
            throw new NotOrganizationMemberException();
        }

        // 이미 세션에 참여한 유저면 예외발생
        boolean alreadyParticipant = participantRepository.existsByUserIdAndSessionIdAndDeletedAtIsNull(userId, sessionId);
        if (alreadyParticipant) {
            throw new AlreadySessionParticipantException();
        }

        Participant participant = Participant.builder()
            .userId(userId)
            .session(session)
            .build();

        participantRepository.save(participant);
    }
}
