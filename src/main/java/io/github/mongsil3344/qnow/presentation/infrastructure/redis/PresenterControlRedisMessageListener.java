package io.github.mongsil3344.qnow.presentation.infrastructure.redis;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class PresenterControlRedisMessageListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public PresenterControlRedisMessageListener(
        ObjectMapper objectMapper,
        SimpMessagingTemplate messagingTemplate
    ) {
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            PresenterControlChangedMessage changedMessage = objectMapper.readValue(
                payload,
                PresenterControlChangedMessage.class
            );
            if (!changedMessage.isRelayable()) {
                return;
            }

            messagingTemplate.convertAndSend(
                destination(changedMessage.sessionId()),
                PresenterControlChangedMessage.of(
                    changedMessage.sessionId(),
                    changedMessage.occurredAt()
                )
            );
        } catch (Exception ignored) {
        }
    }

    private String destination(UUID sessionId) {
        return "/topic/sessions/%s/presenter-view".formatted(sessionId);
    }
}
