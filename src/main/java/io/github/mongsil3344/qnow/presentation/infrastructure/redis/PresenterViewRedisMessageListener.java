package io.github.mongsil3344.qnow.presentation.infrastructure.redis;

import io.github.mongsil3344.qnow.presentation.domain.PresenterViewEvent;
import java.nio.charset.StandardCharsets;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class PresenterViewRedisMessageListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public PresenterViewRedisMessageListener(ObjectMapper objectMapper, SimpMessagingTemplate messagingTemplate) {
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            PresenterViewEvent event = objectMapper.readValue(payload, PresenterViewEvent.class);
            messagingTemplate.convertAndSend(destination(event), event);
        } catch (Exception ignored) {
        }
    }

    private String destination(PresenterViewEvent event) {
        return "/topic/sessions/%s/presenter-view".formatted(event.sessionId());
    }
}
