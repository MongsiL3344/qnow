package io.github.mongsil3344.qnow.user.application;

import io.github.mongsil3344.qnow.user.api.UserQueryApi;
import io.github.mongsil3344.qnow.user.infrastructure.repo.UserRepository;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserQueryImpl implements UserQueryApi {

    private final UserRepository userRepository;

    @Override
    public boolean existsUser(UUID userId) {
        return userRepository.existsByIdAndDeletedAtIsNull(userId);
    }
}
