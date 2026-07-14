package io.github.mongsil3344.qnow.presentation.infrastructure.websocket;

import io.github.mongsil3344.qnow.presentation.application.PresenterViewMetrics;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

// 메트릭 검사용 클래스
@Component
public class PresenterViewWebSocketEventListener {

    private final PresenterViewMetrics metrics;

    public PresenterViewWebSocketEventListener(PresenterViewMetrics metrics) {
        this.metrics = metrics;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            metrics.connected(sessionId);
        }
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        metrics.disconnected(sessionId);
    }
}
