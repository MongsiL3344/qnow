package io.github.mongsil3344.qnow.user.infrastructure.repo;

import io.github.mongsil3344.qnow.user.domain.User;
import io.github.mongsil3344.qnow.user.domain.UserStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmailAndStatusNot(String email, UserStatus status);
    boolean existsByUsernameAndStatusNot(String username, UserStatus status);
}
