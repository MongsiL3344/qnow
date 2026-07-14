package io.github.mongsil3344.qnow.session.infrastructure.repo;

import io.github.mongsil3344.qnow.session.domain.Session;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByIdAndOrganizationIdAndDeletedAtIsNull(UUID id, UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
        select s
        from Session s
        where s.id = :sessionId
            and s.organizationId = :organizationId
            and s.deletedAt is null
        """)
    Optional<Session> findByIdAndOrganizationIdForLifecycleRead(
        @Param("sessionId") UUID sessionId,
        @Param("organizationId") UUID organizationId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select s
        from Session s
        where s.id = :sessionId
            and s.organizationId = :organizationId
            and s.deletedAt is null
        """)
    Optional<Session> findByIdAndOrganizationIdForLifecycleUpdate(
        @Param("sessionId") UUID sessionId,
        @Param("organizationId") UUID organizationId
    );

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
        select s
        from Session s
        where s.id = :sessionId
            and s.deletedAt is null
        """)
    Optional<Session> findByIdForLifecycleRead(@Param("sessionId") UUID sessionId);

    boolean existsByIdAndOrganizationIdAndDeletedAtIsNull(UUID id, UUID organizationId);

    boolean existsByIdAndCreatorIdAndDeletedAtIsNull(UUID id, UUID creatorId);

    List<Session> findAllByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID organizationId);

    @Query("""
        select s.organizationId
        from Session s
        where s.id = :sessionId
            and s.deletedAt is null
        """)
    Optional<UUID> findOrganizationIdBySessionId(@Param("sessionId") UUID sessionId);
}
