package io.github.mongsil3344.qnow.question.infrastructure.repo;

import io.github.mongsil3344.qnow.question.domain.QuestionUpvote;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionUpvoteRepository extends JpaRepository<QuestionUpvote, UUID> {

    Optional<QuestionUpvote> findByQuestionIdAndVoterUserId(UUID questionId, UUID voterUserId);

    @Query("""
        select questionUpvote.question.id
        from QuestionUpvote questionUpvote
        where questionUpvote.question.id in :questionIds
            and questionUpvote.voterUserId = :voterUserId
        """)
    List<UUID> findUpvotedQuestionIds(
        @Param("questionIds") Collection<UUID> questionIds,
        @Param("voterUserId") UUID voterUserId
    );
}
