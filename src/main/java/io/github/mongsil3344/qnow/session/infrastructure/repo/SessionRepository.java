package io.github.mongsil3344.qnow.session.infrastructure.repo;

import io.github.mongsil3344.qnow.session.domain.Session;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, UUID> {

}
