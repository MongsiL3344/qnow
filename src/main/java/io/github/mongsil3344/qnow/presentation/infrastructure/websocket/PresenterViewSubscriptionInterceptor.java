package io.github.mongsil3344.qnow.presentation.infrastructure.websocket;

import io.github.mongsil3344.qnow.presentation.application.PresenterViewMetrics;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.user.api.UserPrincipal;
import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

// 웹소켓으로 들어오는 메세지를 가로채서 먼저 검사
@Component
public class PresenterViewSubscriptionInterceptor implements ChannelInterceptor {

    private static final Pattern DESTINATION_PATTERN = Pattern.compile(
        "^/topic/sessions/([0-9a-fA-F-]{36})/presenter-view$"
    );

    private final SessionQueryApi sessionQueryApi;
    private final PresenterViewMetrics metrics;

    public PresenterViewSubscriptionInterceptor(SessionQueryApi sessionQueryApi, PresenterViewMetrics metrics) {
        this.sessionQueryApi = sessionQueryApi;
        this.metrics = metrics;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (accessor.getCommand() == StompCommand.SEND) {
            deny("Client STOMP SEND is not allowed");
        }

        if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            validateSubscription(accessor);
        }

        return message;
    }

    private void validateSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        Matcher matcher = DESTINATION_PATTERN.matcher(destination == null ? "" : destination);
        if (!matcher.matches()) {
            deny("Unsupported STOMP subscription destination");
        }

        UUID sessionId;
        try {
            sessionId = UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException exception) {
            deny("Invalid session identifier in STOMP destination");
            return;
        }

        UserPrincipal userPrincipal = extractUserPrincipal(accessor.getUser());
        if (userPrincipal == null || !sessionQueryApi.isActiveParticipant(sessionId, userPrincipal.id())) {
            deny("Active session participant is required");
        }
    }

    private UserPrincipal extractUserPrincipal(Principal principal) {
        if (principal instanceof Authentication authentication
            && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }
        return null;
    }

    private void deny(String reason) {
        metrics.recordSubscriptionDenied();
        throw new AccessDeniedException(reason);
    }
}
