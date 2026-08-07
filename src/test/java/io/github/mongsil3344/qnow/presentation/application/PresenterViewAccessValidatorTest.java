package io.github.mongsil3344.qnow.presentation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.mongsil3344.qnow.presentation.application.exception.PresentationSessionNotFoundException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresenterViewParticipantRequiredException;
import java.time.Instant;
import io.github.mongsil3344.qnow.session.api.SessionAccessApi;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.session.api.SessionEndedException;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.session.api.SessionStatusApi;
import java.util.UUID;
import java.util.Optional;
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

    @Mock
    private PresenterControlStore presenterControlStore;

    private PresenterViewAccessValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PresenterViewAccessValidator(
            sessionQueryApi,
            sessionStatusApi,
            sessionAccessApi,
            presenterControlStore
        );
    }

    @Test
    void 활성_세션의_생성자는_발표자_화면을_제어할_수_있다() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SessionActor actor = new SessionActor.Member(userId);
        when(sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)).thenReturn(true);
        when(sessionQueryApi.findActiveParticipantId(sessionId, actor)).thenReturn(Optional.of(UUID.randomUUID()));
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

    @Test
    void 제어권을_위임받은_참여자는_만료시각과_함께_제어할_수_있다() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-08-05T12:00:00Z");
        SessionActor actor = new SessionActor.Member(userId);
        when(sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)).thenReturn(true);
        when(sessionQueryApi.findActiveParticipantId(sessionId, actor)).thenReturn(Optional.of(participantId));
        when(presenterControlStore.getExpiry(sessionId, participantId)).thenReturn(Optional.of(expiresAt));

        PresenterViewAccessValidator.PresenterControlStatus status = validator.getControlStatus(
            organizationId,
            sessionId,
            actor
        );

        assertThat(status.creator()).isFalse();
        assertThat(status.canControl()).isTrue();
        assertThat(status.controlExpiresAt()).isEqualTo(expiresAt);
        assertThat(status.participantId()).isEqualTo(participantId);
    }
}
