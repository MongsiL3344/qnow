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
        String payload = new String(message.getBody(), StandardCharsets.UTF_8); // 루아에서 보내준 이벤트 객체를 String(json)으로 받기
        try {
            PresenterViewEvent event = objectMapper.readValue(payload, PresenterViewEvent.class); // PresenterViewEvent 객체로 매핑하기 (역직렬화)
            messagingTemplate.convertAndSend(destination(event), event); // event 객체의 토픽으로 event객체를 전송
        } catch (Exception ignored) {
        }
    }

    private String destination(PresenterViewEvent event) {
        return "/topic/sessions/%s/presenter-view".formatted(event.sessionId());
    }
}
