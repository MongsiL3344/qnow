package io.github.mongsil3344.qnow.presentation.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class PresenterControlRedisMessageListenerTest {

    @Mock
    private Message message;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void 제어권_변경_이벤트는_type을_보존해_해당_세션_토픽으로_전달된다() {
        PresenterControlRedisMessageListener listener = new PresenterControlRedisMessageListener(
            objectMapper,
            messagingTemplate
        );
        UUID sessionId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-05T10:20:30Z");
        String payload = objectMapper.writeValueAsString(new WireMessage(
            "PRESENTER_CONTROL_CHANGED",
            sessionId,
            occurredAt
        ));
        when(message.getBody()).thenReturn(payload.getBytes(StandardCharsets.UTF_8));

        listener.onMessage(message, null);

        ArgumentCaptor<Object> relayed = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(
            org.mockito.ArgumentMatchers.eq("/topic/sessions/%s/presenter-view".formatted(sessionId)),
            relayed.capture()
        );
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(relayed.getValue()));
        assertThat(json.get("type").asString()).isEqualTo("PRESENTER_CONTROL_CHANGED");
        assertThat(json.get("sessionId").asString()).isEqualTo(sessionId.toString());
        assertThat(json.get("occurredAt").asString()).isEqualTo(occurredAt.toString());
    }

    @Test
    void 세션_식별자가_없는_메시지는_전달하지_않는다() {
        PresenterControlRedisMessageListener listener = new PresenterControlRedisMessageListener(
            objectMapper,
            messagingTemplate
        );
        String payload = objectMapper.writeValueAsString(new WireMessage(
            "PRESENTER_CONTROL_CHANGED",
            null,
            Instant.parse("2026-08-05T10:20:30Z")
        ));
        when(message.getBody()).thenReturn(payload.getBytes(StandardCharsets.UTF_8));

        listener.onMessage(message, null);

        verifyNoInteractions(messagingTemplate);
    }

    private record WireMessage(String type, UUID sessionId, Instant occurredAt) {
    }
}
