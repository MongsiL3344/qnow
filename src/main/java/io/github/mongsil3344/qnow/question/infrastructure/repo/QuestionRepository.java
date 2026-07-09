package io.github.mongsil3344.qnow.question.infrastructure.repo;

import io.github.mongsil3344.qnow.question.domain.Question;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
}
