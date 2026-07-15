package io.github.mongsil3344.qnow.session.infrastructure.repo;

import io.github.mongsil3344.qnow.session.domain.SessionParticipateCode;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionParticipateCodeRepository extends JpaRepository<SessionParticipateCode, UUID> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
        select participateCode
        from SessionParticipateCode participateCode
        join fetch participateCode.session session
        where participateCode.code = :code
            and participateCode.deletedAt is null
            and session.deletedAt is null
        """)
    Optional<SessionParticipateCode> findActiveByCodeForJoin(@Param("code") String code);

    @Query("""
        select participateCode
        from SessionParticipateCode participateCode
        where participateCode.session.id = :sessionId
            and participateCode.deletedAt is null
            and participateCode.session.deletedAt is null
        """)
    Optional<SessionParticipateCode> findActiveBySessionId(@Param("sessionId") UUID sessionId);
}
