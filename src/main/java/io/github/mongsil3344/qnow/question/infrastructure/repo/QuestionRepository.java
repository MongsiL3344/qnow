package io.github.mongsil3344.qnow.question.infrastructure.repo;

import io.github.mongsil3344.qnow.question.domain.Question;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    Slice<Question> findAllByPresentationIdAndDeletedAtIsNull(UUID presentationId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select question
        from Question question
        where question.id = :questionId
            and question.deletedAt is null
        """)
    Optional<Question> findActiveByIdForUpdate(@Param("questionId") UUID questionId);
}
