package io.github.mongsil3344.qnow.user.application;

import io.github.mongsil3344.qnow.user.api.UserQueryApi;
import io.github.mongsil3344.qnow.user.domain.User;
import io.github.mongsil3344.qnow.user.infrastructure.repo.UserRepository;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class UserQueryImpl implements UserQueryApi {

    private final UserRepository userRepository;

    @Override
    public boolean existsUser(UUID userId) {
        return userRepository.existsByIdAndDeletedAtIsNull(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> findNicknamesByIds(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllByIdInAndDeletedAtIsNull(userIds).stream()
            .collect(Collectors.toMap(User::getId, User::getNickname));
    }
}
