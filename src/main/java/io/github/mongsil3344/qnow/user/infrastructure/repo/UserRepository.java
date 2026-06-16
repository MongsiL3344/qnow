package io.github.mongsil3344.qnow.user.infrastructure.repo;

import io.github.mongsil3344.qnow.user.domain.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByIdAndDeletedAtIsNull(UUID id);
}
