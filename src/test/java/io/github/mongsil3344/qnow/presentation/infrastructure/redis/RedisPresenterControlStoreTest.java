package io.github.mongsil3344.qnow.presentation.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers(disabledWithoutDocker = true)
class RedisPresenterControlStoreTest {

    private static final int REDIS_PORT = 6379;
    private static final String CHANNEL = "qnow:test:presenter-view:control-events";
    private static final Duration TTL = Duration.ofHours(24);

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(
        DockerImageName.parse("redis:7.4-alpine")
    ).withExposedPorts(REDIS_PORT);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static ObjectMapper objectMapper;
    private static RedisPresenterControlStore controlStore;

    @BeforeAll
    static void setUpRedis() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(
            REDIS.getHost(),
            REDIS.getMappedPort(REDIS_PORT)
        );
        connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        objectMapper = JsonMapper.builder().findAndAddModules().build();
        controlStore = new RedisPresenterControlStore(redisTemplate, objectMapper, TTL, CHANNEL);
    }

    @AfterAll
    static void tearDownRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void 제어권을_부여하면_만료시각이_저장되고_이벤트가_발행된다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        Instant expiresAt = Instant.ofEpochMilli(System.currentTimeMillis() + 600_000);

        try (EventSubscription subscription = subscribe(1)) {
            controlStore.grant(sessionId, participantId, expiresAt);

            assertThat(controlStore.getExpiry(sessionId, participantId)).hasValue(expiresAt);
            assertThat(redisTemplate.getExpire(key(sessionId), TimeUnit.MILLISECONDS))
                .isPositive()
                .isLessThanOrEqualTo(TTL.toMillis());

            Map<String, Object> event = subscription.awaitSingleEvent();
            assertThat(event).containsEntry("type", "PRESENTER_CONTROL_CHANGED");
            assertThat(event.get("sessionId")).isEqualTo(sessionId.toString());
        }
    }

    @Test
    void 만료된_제어권은_조회시_제거된다() {
        UUID sessionId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        controlStore.grant(sessionId, participantId, Instant.now().minusSeconds(1));

        assertThat(controlStore.getExpiry(sessionId, participantId)).isEmpty();
        assertThat(redisTemplate.opsForZSet().score(key(sessionId), participantId.toString())).isNull();
    }

    @Test
    void 제어권_해제시_이벤트가_발행된다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        controlStore.grant(
            sessionId,
            participantId,
            Instant.ofEpochMilli(System.currentTimeMillis() + 600_000)
        );

        try (EventSubscription subscription = subscribe(1)) {
            assertThat(controlStore.revoke(sessionId, participantId)).isTrue();
            assertThat(subscription.awaitSingleEvent()).containsEntry("type", "PRESENTER_CONTROL_CHANGED");
        }
    }

    @Test
    void 해제할_제어권이_없으면_이벤트를_발행하지_않는다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();

        try (EventSubscription subscription = subscribe(1)) {
            assertThat(controlStore.revoke(sessionId, participantId)).isFalse();
            assertThat(subscription.latch().await(300, TimeUnit.MILLISECONDS)).isFalse();
            assertThat(subscription.payloads()).isEmpty();
        }
    }

    @Test
    void 세션_정리시_키가_삭제된다() {
        UUID sessionId = UUID.randomUUID();
        controlStore.grant(
            sessionId,
            UUID.randomUUID(),
            Instant.ofEpochMilli(System.currentTimeMillis() + 600_000)
        );
        assertThat(redisTemplate.hasKey(key(sessionId))).isTrue();

        controlStore.clearSession(sessionId);

        assertThat(redisTemplate.hasKey(key(sessionId))).isFalse();
    }

    private static EventSubscription subscribe(int expectedEventCount) {
        CountDownLatch latch = new CountDownLatch(expectedEventCount);
        List<String> payloads = new CopyOnWriteArrayList<>();
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            payloads.add(new String(message.getBody(), StandardCharsets.UTF_8));
            latch.countDown();
        }, new ChannelTopic(CHANNEL));
        container.afterPropertiesSet();
        container.start();
        return new EventSubscription(container, latch, payloads);
    }

    private static String key(UUID sessionId) {
        return "qnow:presenter-view:controllers:" + sessionId;
    }

    private record EventSubscription(
        RedisMessageListenerContainer container,
        CountDownLatch latch,
        List<String> payloads
    ) implements AutoCloseable {

        Map<String, Object> awaitSingleEvent() throws Exception {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(payloads).hasSize(1);
            return objectMapper.readValue(
                payloads.getFirst(),
                new TypeReference<Map<String, Object>>() {}
            );
        }

        @Override
        public void close() throws Exception {
            container.stop();
            container.destroy();
        }
    }
}
