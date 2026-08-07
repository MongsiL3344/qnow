package io.github.mongsil3344.qnow.presentation.infrastructure.redis;

import io.github.mongsil3344.qnow.presentation.application.PresenterControlStore;
import io.github.mongsil3344.qnow.presentation.application.exception.PresenterViewUnavailableException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RedisPresenterControlStore implements PresenterControlStore {

    private static final String KEY_PREFIX = "qnow:presenter-view:controllers:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private final String channel;

    public RedisPresenterControlStore(
        StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper,
        @Value("${qnow.presenter-view.ttl:24h}") Duration ttl,
        @Value("${qnow.presenter-view.control-channel:qnow:presenter-view:control-events}") String channel
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
        this.channel = channel;
    }

    @Override
    public void grant(UUID sessionId, UUID participantId, Instant expiresAt) {
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(participantId);
        Objects.requireNonNull(expiresAt);

        try {
            String key = key(sessionId);
            redisTemplate.opsForZSet().add(key, participantId.toString(), expiresAt.toEpochMilli());
            redisTemplate.expire(key, ttl);
            publish(sessionId, Instant.now());
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public boolean revoke(UUID sessionId, UUID participantId) {
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(participantId);

        try {
            Long removed = redisTemplate.opsForZSet().remove(key(sessionId), participantId.toString());
            if (removed != null && removed > 0) {
                publish(sessionId, Instant.now());
                return true;
            }
            return false;
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public Optional<Instant> getExpiry(UUID sessionId, UUID participantId) {
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(participantId);

        try {
            String key = key(sessionId);
            long now = System.currentTimeMillis();
            ZSetOperations<String, String> operations = redisTemplate.opsForZSet();
            operations.removeRangeByScore(key, Double.NEGATIVE_INFINITY, now);
            Double score = operations.score(key, participantId.toString());
            if (score == null) {
                return Optional.empty();
            }
            Instant expiresAt = Instant.ofEpochMilli(score.longValue());

            return expiresAt.isAfter(Instant.ofEpochMilli(now))
                ? Optional.of(expiresAt)
                : Optional.empty();
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public void clearSession(UUID sessionId) {
        Objects.requireNonNull(sessionId);

        try {
            redisTemplate.delete(key(sessionId));
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    private void publish(UUID sessionId, Instant occurredAt) {
        PresenterControlChangedMessage message = PresenterControlChangedMessage.of(
            sessionId,
            occurredAt
        );
        redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(message));
    }

    private PresenterViewUnavailableException unavailable(RuntimeException cause) {
        return new PresenterViewUnavailableException(cause);
    }

    private String key(UUID sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
