package io.github.mongsil3344.qnow.session.infrastructure.repo;

import io.github.mongsil3344.qnow.session.domain.Session;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByIdAndOrganizationIdAndDeletedAtIsNull(UUID id, UUID organizationId);

    boolean existsByIdAndOrganizationIdAndDeletedAtIsNull(UUID id, UUID organizationId);

    List<Session> findAllByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID organizationId);

    @Query("""
        select s.organizationId
        from Session s
        where s.id = :sessionId
            and s.deletedAt is null
        """)
    Optional<UUID> findOrganizationIdBySessionId(@Param("sessionId") UUID sessionId);
}
