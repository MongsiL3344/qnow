package io.github.mongsil3344.qnow.session.infrastructure.repo;

import io.github.mongsil3344.qnow.session.domain.Participant;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {

    boolean existsByUserIdAndSessionIdAndDeletedAtIsNull(UUID userId, UUID sessionId);

    Optional<Participant> findByUserIdAndSessionIdAndDeletedAtIsNull(UUID userId, UUID sessionId);

    Optional<Participant> findByIdAndSessionIdAndUserIdIsNullAndDeletedAtIsNull(UUID id, UUID sessionId);

    @Query("""
        select p.id
        from Participant p
        where p.session.id = :sessionId
            and p.userId = :userId
            and p.deletedAt is null
            and p.session.deletedAt is null
            and p.session.endAt is null
        """)
    Optional<UUID> findActiveParticipantId(
        @Param("sessionId") UUID sessionId,
        @Param("userId") UUID userId
    );

    @Query("""
        select p.id
        from Participant p
        where p.session.id = :sessionId
            and p.id = :participantId
            and p.userId is null
            and p.guestNickname is not null
            and p.deletedAt is null
            and p.session.deletedAt is null
            and p.session.endAt is null
        """)
    Optional<UUID> findActiveGuestParticipantId(
        @Param("sessionId") UUID sessionId,
        @Param("participantId") UUID participantId
    );

    @Query("""
        select p.session.id as sessionId,
            count(distinct p.userId)
                + sum(case when p.userId is null then 1 else 0 end) as participantCount
        from Participant p
        where p.session.id in :sessionIds
            and (
                (p.session.endAt is null and p.deletedAt is null)
                or p.session.endAt is not null
            )
        group by p.session.id
        """)
    List<SessionParticipantCount> countParticipantsBySessionIds(@Param("sessionIds") Collection<UUID> sessionIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update Participant p
        set p.deletedAt = :endedAt
        where p.session.id = :sessionId
            and p.deletedAt is null
        """)
    void exitAllActiveParticipants(
        @Param("sessionId") UUID sessionId,
        @Param("endedAt") Instant endedAt
    );

    @Query("""
        select p.id as participantId, p.userId as userId
        from Participant p
        where p.id in :participantIds
            and p.userId is not null
        """)
    List<ParticipantUserId> findUserIdsByParticipantIds(
        @Param("participantIds") Collection<UUID> participantIds
    );

    @Query("""
        select p.id as participantId, p.guestNickname as guestNickname
        from Participant p
        where p.id in :participantIds
            and p.userId is null
            and p.guestNickname is not null
        """)
    List<ParticipantGuestNickname> findGuestNicknamesByParticipantIds(
        @Param("participantIds") Collection<UUID> participantIds
    );

    interface SessionParticipantCount {
        UUID getSessionId();

        long getParticipantCount();
    }

    interface ParticipantUserId {
        UUID getParticipantId();

        UUID getUserId();
    }

    interface ParticipantGuestNickname {
        UUID getParticipantId();

        String getGuestNickname();
    }
}
