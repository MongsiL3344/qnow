package io.github.mongsil3344.qnow.presentation.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.mongsil3344.qnow.presentation.application.PresenterViewMetrics;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.user.api.UserPrincipal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class PresenterViewSubscriptionInterceptorTest {

    @Mock
    private SessionQueryApi sessionQueryApi;

    @Mock
    private PresenterViewMetrics metrics;

    private PresenterViewSubscriptionInterceptor interceptor;
    private MessageChannel channel;

    @BeforeEach
    void setUp() {
        interceptor = new PresenterViewSubscriptionInterceptor(sessionQueryApi, metrics);
        channel = mock(MessageChannel.class);
    }

    @Test
    void 활성_참여자는_발표자_화면_토픽을_구독할_수_있다() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Message<byte[]> message = message(
            StompCommand.SUBSCRIBE,
            destination(sessionId),
            authentication(userId)
        );
        when(sessionQueryApi.isActiveParticipant(sessionId, userId)).thenReturn(true);

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @Test
    void 참여자가_아니면_구독할_수_없다() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Message<byte[]> message = message(
            StompCommand.SUBSCRIBE,
            destination(sessionId),
            authentication(userId)
        );

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Active session participant is required");

        verify(sessionQueryApi).isActiveParticipant(sessionId, userId);
        verify(metrics).recordSubscriptionDenied();
    }

    @Test
    void 인증되지_않은_사용자는_구독할_수_없다() {
        Message<byte[]> message = message(
            StompCommand.SUBSCRIBE,
            destination(UUID.randomUUID()),
            null
        );

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(sessionQueryApi);
        verify(metrics).recordSubscriptionDenied();
    }

    @Test
    void 지원하지_않는_구독_목적지는_거부한다() {
        Message<byte[]> message = message(
            StompCommand.SUBSCRIBE,
            "/topic/sessions/not-a-session/presenter-view",
            authentication(UUID.randomUUID())
        );

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Unsupported STOMP subscription destination");

        verifyNoInteractions(sessionQueryApi);
        verify(metrics).recordSubscriptionDenied();
    }

    @Test
    void 모든_클라이언트_전송_프레임을_거부한다() {
        Message<byte[]> message = message(
            StompCommand.SEND,
            "/topic/sessions/%s/presenter-view".formatted(UUID.randomUUID()),
            authentication(UUID.randomUUID())
        );

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Client STOMP SEND is not allowed");

        verifyNoInteractions(sessionQueryApi);
        verify(metrics).recordSubscriptionDenied();
    }

    private Message<byte[]> message(
        StompCommand command,
        String destination,
        UsernamePasswordAuthenticationToken authentication
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setSessionId(UUID.randomUUID().toString());
        accessor.setUser(authentication);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private UsernamePasswordAuthenticationToken authentication(UUID userId) {
        UserPrincipal principal = new UserPrincipal(
            userId,
            "%s@example.com".formatted(userId),
            "participant",
            "encoded-password"
        );
        return new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());
    }

    private String destination(UUID sessionId) {
        return "/topic/sessions/%s/presenter-view".formatted(sessionId);
    }
}
