package io.github.mongsil3344.qnow.presentation.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.mongsil3344.qnow.session.api.GuestPrincipal;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.session.api.SessionActorResolver;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.user.api.UserPrincipal;
import java.util.List;
import java.util.Optional;
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
    private SessionActorResolver sessionActorResolver;

    private PresenterViewSubscriptionInterceptor interceptor;
    private MessageChannel channel;

    @BeforeEach
    void setUp() {
        interceptor = new PresenterViewSubscriptionInterceptor(
            sessionQueryApi,
            sessionActorResolver
        );
        channel = mock(MessageChannel.class);
    }

    @Test
    void 활성_참여자는_발표자_화면_토픽을_구독할_수_있다() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken authentication = authentication(userId);
        SessionActor actor = new SessionActor.Member(userId);
        Message<byte[]> message = message(
            StompCommand.SUBSCRIBE,
            destination(sessionId),
            authentication
        );
        when(sessionActorResolver.resolve(authentication)).thenReturn(Optional.of(actor));
        when(sessionQueryApi.isActiveParticipant(sessionId, actor)).thenReturn(true);

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @Test
    void 활성_비회원_참여자는_발표자_화면_토픽을_구독할_수_있다() {
        UUID sessionId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken authentication = guestAuthentication(sessionId, participantId);
        SessionActor actor = new SessionActor.Guest(participantId, sessionId);
        Message<byte[]> message = message(
            StompCommand.SUBSCRIBE,
            destination(sessionId),
            authentication
        );
        when(sessionActorResolver.resolve(authentication)).thenReturn(Optional.of(actor));
        when(sessionQueryApi.isActiveParticipant(sessionId, actor)).thenReturn(true);

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @Test
    void 참여자가_아니면_구독할_수_없다() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken authentication = authentication(userId);
        SessionActor actor = new SessionActor.Member(userId);
        Message<byte[]> message = message(
            StompCommand.SUBSCRIBE,
            destination(sessionId),
            authentication
        );
        when(sessionActorResolver.resolve(authentication)).thenReturn(Optional.of(actor));

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Active session participant is required");

        verify(sessionQueryApi).isActiveParticipant(sessionId, actor);
    }

    @Test
    void 인증되지_않은_사용자는_구독할_수_없다() {
        Message<byte[]> message = message(
            StompCommand.SUBSCRIBE,
            destination(UUID.randomUUID()),
            null
        );
        when(sessionActorResolver.resolve(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(sessionQueryApi);
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

    private UsernamePasswordAuthenticationToken guestAuthentication(UUID sessionId, UUID participantId) {
        GuestPrincipal principal = new GuestPrincipal(participantId, sessionId);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private String destination(UUID sessionId) {
        return "/topic/sessions/%s/presenter-view".formatted(sessionId);
    }
}
