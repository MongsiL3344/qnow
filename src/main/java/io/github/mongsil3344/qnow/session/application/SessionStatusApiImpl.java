package io.github.mongsil3344.qnow.session.application;

import io.github.mongsil3344.qnow.session.api.SessionEndedException;
import io.github.mongsil3344.qnow.session.api.SessionStatusApi;
import io.github.mongsil3344.qnow.session.application.exception.SessionNotFoundException;
import io.github.mongsil3344.qnow.session.domain.Session;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionRepository;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Component
public class SessionStatusApiImpl implements SessionStatusApi {

    private final SessionRepository sessionRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY) // 상위 트랜잭션에 합류하는 트랜잭션 설정 (이 메서드를 호출하는 곳은 트랜잭션이어야함)
    public void requireNotEnded(UUID sessionId) {
        Session session = sessionRepository.findByIdForLifecycleRead(sessionId)
            .orElseThrow(SessionNotFoundException::new);

        if (session.isEnded()) {
            throw new SessionEndedException();
        }
    }
}
