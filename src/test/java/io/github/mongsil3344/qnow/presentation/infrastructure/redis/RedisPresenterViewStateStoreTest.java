package io.github.mongsil3344.qnow.presentation.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mongsil3344.qnow.presentation.application.PresenterViewMetrics;
import io.github.mongsil3344.qnow.presentation.application.PresenterViewUpdateResult;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewClearReason;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewEvent;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewEventType;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewSnapshot;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers(disabledWithoutDocker = true)
class RedisPresenterViewStateStoreTest {

    private static final int REDIS_PORT = 6379;
    private static final String CHANNEL = "qnow:test:presenter-view:events";
    private static final Duration TTL = Duration.ofHours(24);

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(
        DockerImageName.parse("redis:7.4-alpine")
    ).withExposedPorts(REDIS_PORT);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static ObjectMapper objectMapper;
    private static RedisPresenterViewStateStore stateStore;

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
        stateStore = new RedisPresenterViewStateStore(
            redisTemplate,
            objectMapper,
            new PresenterViewMetrics(new SimpleMeterRegistry()),
            TTL,
            CHANNEL
        );
    }

    @AfterAll
    static void tearDownRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void 첫_변경은_스냅샷을_저장하고_TTL을_갱신하며_같은_리비전을_발행한다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID presentationId = UUID.randomUUID();
        Instant updatedAt = Instant.parse("2026-07-13T10:20:30Z");

        try (EventSubscription subscription = subscribe(1)) {
            PresenterViewUpdateResult result = stateStore.update(sessionId, presentationId, 12, updatedAt);

            assertThat(result.changed()).isTrue();
            assertThat(result.snapshot()).isEqualTo(new PresenterViewSnapshot(
                sessionId,
                presentationId,
                12,
                1,
                updatedAt
            ));
            assertThat(stateStore.get(sessionId)).isEqualTo(result.snapshot());

            Long remainingTtl = redisTemplate.getExpire(key(sessionId), TimeUnit.MILLISECONDS);
            assertThat(remainingTtl).isPositive().isLessThanOrEqualTo(TTL.toMillis());

            PresenterViewEvent event = subscription.awaitSingleEvent();
            assertThat(event.type()).isEqualTo(PresenterViewEventType.PRESENTER_VIEW_UPDATED);
            assertThat(event.toSnapshot()).isEqualTo(result.snapshot());
        }
    }

    @Test
    void 같은_위치로_변경하면_아무_작업도_하지_않고_발행_없이_리비전을_유지한다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID presentationId = UUID.randomUUID();
        Instant firstUpdate = Instant.parse("2026-07-13T10:20:30Z");

        PresenterViewUpdateResult first = stateStore.update(sessionId, presentationId, 3, firstUpdate);
        try (EventSubscription subscription = subscribe(1)) {
            PresenterViewUpdateResult duplicate = stateStore.update(
                sessionId,
                presentationId,
                3,
                firstUpdate.plusSeconds(30)
            );

            assertThat(duplicate.changed()).isFalse();
            assertThat(duplicate.snapshot()).isEqualTo(first.snapshot());
            assertThat(duplicate.snapshot().revision()).isEqualTo(1);
            assertThat(subscription.latch().await(300, TimeUnit.MILLISECONDS)).isFalse();
            assertThat(subscription.payloads()).isEmpty();
        }
    }

    @Test
    void 동시_변경은_서로_다르고_단조_증가하는_리비전을_받는다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID presentationId = UUID.randomUUID();
        Instant baseTime = Instant.parse("2026-07-13T10:20:30Z");
        ExecutorService executor = Executors.newFixedThreadPool(8);

        try {
            List<Future<PresenterViewUpdateResult>> futures = new ArrayList<>();
            for (int page = 1; page <= 20; page++) {
                int requestedPage = page;
                futures.add(executor.submit(() -> stateStore.update(
                    sessionId,
                    presentationId,
                    requestedPage,
                    baseTime.plusMillis(requestedPage)
                )));
            }

            List<Long> revisions = new ArrayList<>();
            for (Future<PresenterViewUpdateResult> future : futures) {
                revisions.add(future.get(5, TimeUnit.SECONDS).snapshot().revision());
            }

            assertThat(revisions).containsExactlyInAnyOrderElementsOf(
                java.util.stream.LongStream.rangeClosed(1, 20).boxed().toList()
            );
            assertThat(stateStore.get(sessionId).revision()).isEqualTo(20);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void 현재_발표자료를_초기화하면_리비전이_있는_툼스톤을_남긴다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID presentationId = UUID.randomUUID();
        Instant updatedAt = Instant.parse("2026-07-13T10:20:30Z");
        stateStore.update(sessionId, presentationId, 4, updatedAt);

        try (EventSubscription subscription = subscribe(1)) {
            Optional<PresenterViewSnapshot> cleared = stateStore.clearPresentation(
                sessionId,
                presentationId,
                updatedAt.plusSeconds(10),
                PresenterViewClearReason.PRESENTATION_DELETED
            );

            assertThat(cleared).hasValueSatisfying(snapshot -> {
                assertThat(snapshot.hasView()).isFalse();
                assertThat(snapshot.revision()).isEqualTo(2);
            });
            assertThat(stateStore.get(sessionId)).isEqualTo(cleared.orElseThrow());

            PresenterViewEvent event = subscription.awaitSingleEvent();
            assertThat(event.type()).isEqualTo(PresenterViewEventType.PRESENTER_VIEW_CLEARED);
            assertThat(event.reason()).isEqualTo(PresenterViewClearReason.PRESENTATION_DELETED);
            assertThat(event.revision()).isEqualTo(2);
        }
    }

    @Test
    void 다른_발표자료를_초기화하면_아무_작업도_하지_않는다() {
        UUID sessionId = UUID.randomUUID();
        stateStore.update(
            sessionId,
            UUID.randomUUID(),
            1,
            Instant.parse("2026-07-13T10:20:30Z")
        );

        Optional<PresenterViewSnapshot> result = stateStore.clearPresentation(
            sessionId,
            UUID.randomUUID(),
            Instant.parse("2026-07-13T10:21:00Z"),
            PresenterViewClearReason.PRESENTATION_DELETED
        );

        assertThat(result).isEmpty();
        assertThat(stateStore.get(sessionId).revision()).isEqualTo(1);
    }

    @Test
    void 세션을_초기화하면_다음_리비전을_발행하고_스냅샷을_삭제한다() throws Exception {
        UUID sessionId = UUID.randomUUID();
        stateStore.update(
            sessionId,
            UUID.randomUUID(),
            2,
            Instant.parse("2026-07-13T10:20:30Z")
        );

        try (EventSubscription subscription = subscribe(1)) {
            stateStore.clearSession(
                sessionId,
                Instant.parse("2026-07-13T10:21:00Z"),
                PresenterViewClearReason.SESSION_ENDED
            );

            assertThat(redisTemplate.hasKey(key(sessionId))).isFalse();
            assertThat(stateStore.get(sessionId)).isEqualTo(PresenterViewSnapshot.empty(sessionId));

            PresenterViewEvent event = subscription.awaitSingleEvent();
            assertThat(event.type()).isEqualTo(PresenterViewEventType.PRESENTER_VIEW_CLEARED);
            assertThat(event.reason()).isEqualTo(PresenterViewClearReason.SESSION_ENDED);
            assertThat(event.revision()).isEqualTo(2);
        }
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
        return "qnow:presenter-view:session:" + sessionId;
    }

    private record EventSubscription(
        RedisMessageListenerContainer container,
        CountDownLatch latch,
        List<String> payloads
    ) implements AutoCloseable {

        PresenterViewEvent awaitSingleEvent() throws Exception {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(payloads).hasSize(1);
            return objectMapper.readValue(payloads.getFirst(), PresenterViewEvent.class);
        }

        @Override
        public void close() throws Exception {
            container.stop();
            container.destroy();
        }
    }
}
