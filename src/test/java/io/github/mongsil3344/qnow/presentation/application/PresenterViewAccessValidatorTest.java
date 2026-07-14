package io.github.mongsil3344.qnow.presentation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.mongsil3344.qnow.presentation.application.exception.PresentationSessionNotFoundException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresenterViewParticipantRequiredException;
import io.github.mongsil3344.qnow.session.api.SessionAccessApi;
import io.github.mongsil3344.qnow.session.api.SessionEndedException;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.session.api.SessionStatusApi;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PresenterViewAccessValidatorTest {

    @Mock
    private SessionQueryApi sessionQueryApi;

    @Mock
    private SessionStatusApi sessionStatusApi;

    @Mock
    private SessionAccessApi sessionAccessApi;

    private PresenterViewAccessValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PresenterViewAccessValidator(sessionQueryApi, sessionStatusApi, sessionAccessApi);
    }

    @Test
    void activeSessionCreatorCanControlPresenterView() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)).thenReturn(true);
        when(sessionQueryApi.isActiveParticipant(sessionId, userId)).thenReturn(true);
        when(sessionAccessApi.isSessionCreator(sessionId, userId)).thenReturn(true);

        assertThat(validator.isSessionCreator(organizationId, sessionId, userId)).isTrue();
    }

    @Test
    void missingSessionIsRejected() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> validator.isSessionCreator(organizationId, sessionId, userId))
            .isInstanceOf(PresentationSessionNotFoundException.class);

        verifyNoInteractions(sessionStatusApi, sessionAccessApi);
    }

    @Test
    void endedSessionIsRejected() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)).thenReturn(true);
        doThrow(new SessionEndedException()).when(sessionStatusApi).requireNotEnded(sessionId);

        assertThatThrownBy(() -> validator.isSessionCreator(organizationId, sessionId, userId))
            .isInstanceOf(SessionEndedException.class);
    }

    @Test
    void inactiveParticipantIsRejected() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)).thenReturn(true);

        assertThatThrownBy(() -> validator.isSessionCreator(organizationId, sessionId, userId))
            .isInstanceOf(PresenterViewParticipantRequiredException.class);

        verifyNoInteractions(sessionAccessApi);
    }
}
