package io.github.mongsil3344.qnow.presentation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.mongsil3344.qnow.presentation.application.exception.PresentationSessionNotFoundException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresenterViewParticipantRequiredException;
import io.github.mongsil3344.qnow.session.api.SessionAccessApi;
import io.github.mongsil3344.qnow.session.api.SessionActor;
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
    void 활성_세션의_생성자는_발표자_화면을_제어할_수_있다() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SessionActor actor = new SessionActor.Member(userId);
        when(sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)).thenReturn(true);
        when(sessionQueryApi.isActiveParticipant(sessionId, actor)).thenReturn(true);
        when(sessionAccessApi.isSessionCreator(sessionId, userId)).thenReturn(true);

        assertThat(validator.isSessionCreator(organizationId, sessionId, userId)).isTrue();
    }

    @Test
    void 세션이_없으면_거부한다() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> validator.isSessionCreator(organizationId, sessionId, userId))
            .isInstanceOf(PresentationSessionNotFoundException.class);

        verifyNoInteractions(sessionStatusApi, sessionAccessApi);
    }

    @Test
    void 종료된_세션은_거부한다() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)).thenReturn(true);
        doThrow(new SessionEndedException()).when(sessionStatusApi).requireNotEnded(sessionId);

        assertThatThrownBy(() -> validator.isSessionCreator(organizationId, sessionId, userId))
            .isInstanceOf(SessionEndedException.class);
    }

    @Test
    void 비활성_참여자는_거부한다() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)).thenReturn(true);

        assertThatThrownBy(() -> validator.isSessionCreator(organizationId, sessionId, userId))
            .isInstanceOf(PresenterViewParticipantRequiredException.class);

        verifyNoInteractions(sessionAccessApi);
    }
}
