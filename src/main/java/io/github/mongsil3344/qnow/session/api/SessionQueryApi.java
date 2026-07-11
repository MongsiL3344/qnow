package io.github.mongsil3344.qnow.session.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SessionQueryApi {

    List<SessionSummary> findSessionSummariesByOrganizationId(UUID organizationId);

    boolean existsSessionInOrganization(UUID sessionId, UUID organizationId);

    boolean isActiveParticipant(UUID sessionId, UUID userId);

    Optional<UUID> findActiveParticipantId(UUID sessionId, UUID userId);

    Optional<UUID> findOrganizationIdBySessionId(UUID sessionId);

    Map<UUID, UUID> findUserIdsByParticipantIds(Collection<UUID> participantIds);
}
