package io.github.mongsil3344.qnow.presentation.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import io.github.mongsil3344.qnow.presentation.application.event.PresentationDeletedEvent;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewClearReason;
import io.github.mongsil3344.qnow.session.api.SessionEndedEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PresenterViewLifecycleListenerTest {

    @Mock
    private PresenterViewStateStore stateStore;

    private PresenterViewLifecycleListener listener;

    @BeforeEach
    void setUp() {
        listener = new PresenterViewLifecycleListener(stateStore);
    }

    @Test
    void sessionEndClearsPresenterView() {
        UUID sessionId = UUID.randomUUID();

        listener.on(new SessionEndedEvent(sessionId));

        verify(stateStore).clearSession(
            org.mockito.ArgumentMatchers.eq(sessionId),
            any(Instant.class),
            org.mockito.ArgumentMatchers.eq(PresenterViewClearReason.SESSION_ENDED)
        );
    }

    @Test
    void presentationDeletionClearsMatchingPresenterView() {
        UUID sessionId = UUID.randomUUID();
        UUID presentationId = UUID.randomUUID();

        listener.on(new PresentationDeletedEvent(sessionId, presentationId));

        verify(stateStore).clearPresentation(
            org.mockito.ArgumentMatchers.eq(sessionId),
            org.mockito.ArgumentMatchers.eq(presentationId),
            any(Instant.class),
            org.mockito.ArgumentMatchers.eq(PresenterViewClearReason.PRESENTATION_DELETED)
        );
    }
}
