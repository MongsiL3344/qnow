package io.github.mongsil3344.qnow.session.application;

import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.session.api.SessionSummary;
import io.github.mongsil3344.qnow.session.domain.Session;
import io.github.mongsil3344.qnow.session.infrastructure.repo.ParticipantRepository;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Component
public class SessionQueryApiImpl implements SessionQueryApi {

    private final SessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SessionSummary> findSessionSummariesByOrganizationId(UUID organizationId) {
        List<Session> sessions = sessionRepository.findAllByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            organizationId
        );

        if (sessions.isEmpty()) {
            return List.of();
        }

        Map<UUID, Long> participantCounts = getParticipantCounts(sessions);

        return sessions.stream()
            .map(session -> new SessionSummary(
                session.getId(),
                session.getTitle(),
                session.getCreatorId(),
                session.getStartAt(),
                session.getEndAt(),
                participantCounts.getOrDefault(session.getId(), 0L)
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsSessionInOrganization(UUID sessionId, UUID organizationId) {
        return sessionRepository.existsByIdAndOrganizationIdAndDeletedAtIsNull(sessionId, organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveParticipant(UUID sessionId, UUID userId) {
        return participantRepository.existsActiveParticipant(sessionId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findActiveParticipantId(UUID sessionId, UUID userId) {
        return participantRepository.findActiveParticipantId(sessionId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findOrganizationIdBySessionId(UUID sessionId) {
        return sessionRepository.findOrganizationIdBySessionId(sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, UUID> findUserIdsByParticipantIds(Collection<UUID> participantIds) {
        if (participantIds.isEmpty()) {
            return Map.of();
        }

        return participantRepository.findUserIdsByParticipantIds(participantIds).stream()
            .collect(Collectors.toMap(
                ParticipantRepository.ParticipantUserId::getParticipantId,
                ParticipantRepository.ParticipantUserId::getUserId
            ));
    }

    private Map<UUID, Long> getParticipantCounts(List<Session> sessions) {
        List<UUID> sessionIds = sessions.stream()
            .map(Session::getId)
            .toList();

        return participantRepository.countParticipantsBySessionIds(sessionIds).stream()
            .collect(Collectors.toMap(
                ParticipantRepository.SessionParticipantCount::getSessionId,
                ParticipantRepository.SessionParticipantCount::getParticipantCount
            ));
    }
}
