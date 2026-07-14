package io.github.mongsil3344.qnow.presentation.application;

import io.github.mongsil3344.qnow.presentation.application.event.PresentationDeletedEvent;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewClearReason;
import io.github.mongsil3344.qnow.session.api.SessionEndedEvent;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

// Application에서 던질 이벤트의 리스너
@AllArgsConstructor
@Component
@ConditionalOnProperty(
    prefix = "qnow.presenter-view",
    name = "realtime-enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class PresenterViewLifecycleListener {

    private final PresenterViewStateStore stateStore;

    // 파라미터로 들어오는 객체의 타입에 따라 어떤 리스너가 실행될지 자동으로 결정됨
    @ApplicationModuleListener
    public void on(SessionEndedEvent event) {
        stateStore.clearSession(
            event.sessionId(),
            Instant.now(),
            PresenterViewClearReason.SESSION_ENDED
        );
    }

    @ApplicationModuleListener
    public void on(PresentationDeletedEvent event) {
        stateStore.clearPresentation(
            event.sessionId(),
            event.presentationId(),
            Instant.now(),
            PresenterViewClearReason.PRESENTATION_DELETED
        );
    }
}
