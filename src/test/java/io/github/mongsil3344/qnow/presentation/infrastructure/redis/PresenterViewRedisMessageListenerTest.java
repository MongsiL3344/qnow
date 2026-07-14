package io.github.mongsil3344.qnow.presentation.infrastructure.redis;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.mongsil3344.qnow.presentation.domain.PresenterViewEvent;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewEventType;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class PresenterViewRedisMessageListenerTest {

    @Mock
    private Message message;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void 레디스_이벤트는_해당_세션_토픽으로만_전달된다() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        PresenterViewRedisMessageListener listener = new PresenterViewRedisMessageListener(
            objectMapper,
            messagingTemplate
        );
        PresenterViewEvent event = new PresenterViewEvent(
            PresenterViewEventType.PRESENTER_VIEW_UPDATED,
            UUID.randomUUID(),
            UUID.randomUUID(),
            9,
            17,
            Instant.parse("2026-07-13T10:20:30Z"),
            null
        );
        when(message.getBody()).thenReturn(
            objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8)
        );

        listener.onMessage(message, null);

        verify(messagingTemplate).convertAndSend(
            "/topic/sessions/%s/presenter-view".formatted(event.sessionId()),
            event
        );
    }
}
