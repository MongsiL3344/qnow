package io.github.mongsil3344.qnow.session.infrastructure.repo;

import io.github.mongsil3344.qnow.session.domain.Participant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {

    boolean existsByUserIdAndSessionIdAndDeletedAtIsNull(UUID userId, UUID sessionId);

    Optional<Participant> findByUserIdAndSessionIdAndDeletedAtIsNull(UUID userId, UUID sessionId);

    @Query("""
        select p.session.id as sessionId, count(p.id) as participantCount
        from Participant p
        where p.session.id in :sessionIds
            and p.deletedAt is null
        group by p.session.id
        """)
    List<SessionParticipantCount> countParticipantsBySessionIds(@Param("sessionIds") Collection<UUID> sessionIds);

    interface SessionParticipantCount {
        UUID getSessionId();

        long getParticipantCount();
    }
}
