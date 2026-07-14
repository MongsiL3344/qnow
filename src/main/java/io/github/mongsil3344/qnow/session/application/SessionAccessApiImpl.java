package io.github.mongsil3344.qnow.session.application;

import io.github.mongsil3344.qnow.session.api.SessionAccessApi;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionRepository;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Component
public class SessionAccessApiImpl implements SessionAccessApi {

    private final SessionRepository sessionRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isSessionCreator(UUID sessionId, UUID userId) {
        return sessionRepository.existsByIdAndCreatorIdAndDeletedAtIsNull(sessionId, userId);
    }
}
