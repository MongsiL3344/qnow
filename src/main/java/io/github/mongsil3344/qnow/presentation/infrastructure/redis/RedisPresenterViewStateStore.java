package io.github.mongsil3344.qnow.presentation.infrastructure.redis;

import io.github.mongsil3344.qnow.presentation.application.PresenterViewStateStore;
import io.github.mongsil3344.qnow.presentation.application.exception.PresenterViewUnavailableException;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewClearReason;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewEvent;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

@Component
public class RedisPresenterViewStateStore implements PresenterViewStateStore {

    private static final String KEY_PREFIX = "qnow:presenter-view:session:";
    private static final List<Object> HASH_FIELDS = List.of(
        "presentationId",
        "pageNumber",
        "sequence",
        "updatedAt"
    );

    private static final RedisScript<String> UPDATE_SCRIPT = RedisScript.of(
        new ClassPathResource("redis/scripts/presenter-view/update.lua"),
        String.class
    );
    private static final RedisScript<String> CLEAR_PRESENTATION_SCRIPT = RedisScript.of(
        new ClassPathResource("redis/scripts/presenter-view/clear-presentation.lua"),
        String.class
    );
    private static final RedisScript<String> CLEAR_SESSION_SCRIPT = RedisScript.of(
        new ClassPathResource("redis/scripts/presenter-view/clear-session.lua"),
        String.class
    );

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private final String channel;

    public RedisPresenterViewStateStore(
        StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper,
        @Value("${qnow.presenter-view.ttl:24h}") Duration ttl,
        @Value("${qnow.presenter-view.channel:qnow:presenter-view:events}") String channel
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
        this.channel = channel;
    }

    @Override
    public PresenterViewSnapshot get(UUID sessionId) {
        try {
            // opsFor~() : 뒤에 붙는 자료구조를 다룰 수 있는 객체를 반환함
            // multiGet() : Redis Hash에서 여러 필드 값을 한번에 가져옴, 요청 필드 순서대로 안에 있는 값 매핑
            List<Object> values = redisTemplate.opsForHash().multiGet(key(sessionId), HASH_FIELDS);

            if (values == null || values.size() != HASH_FIELDS.size()) {
                throw new IllegalStateException("Redis snapshot read returned an invalid result");
            }

            // multiGet()으로 가져온 객체의 2번 인덱스(3번쨰 필드)로 접근함
            Object sequenceValue = values.get(2);

            if (sequenceValue == null) {
                return PresenterViewSnapshot.empty(sessionId);
            }

            // get(int index)로 값 꺼냄
            UUID presentationId = parseUuid(values.get(0));
            Integer pageNumber = parseInteger(values.get(1));
            long sequence = Long.parseLong(sequenceValue.toString());
            Instant updatedAt = values.get(3) == null ? null : Instant.parse(values.get(3).toString());

            // 객체 반환
            return new PresenterViewSnapshot(sessionId, presentationId, pageNumber, sequence, updatedAt);
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public PresenterViewSnapshot update(
        UUID sessionId,
        UUID presentationId,
        int pageNumber,
        Instant updatedAt
    ) {
        try {
            // 루아 스크립트 실행 결과를 이벤트 JSON으로 받음
            String result = redisTemplate.execute(
                UPDATE_SCRIPT,                  // 실행할 스크립트
                List.of(key(sessionId)),        // List<K> Keys
                presentationId.toString(),      // ARGV[1]
                Integer.toString(pageNumber),   // ARGV[2]
                updatedAt.toString(),           // ARGV[3]
                Long.toString(ttl.toMillis()),  // ARGV[4]
                channel,                        // ARGV[5]
                sessionId.toString()            // ARGV[6]
            );
            if (!StringUtils.hasText(result)) {
                throw new IllegalStateException("Redis update script returned no result");
            }
            return objectMapper.readValue(result, PresenterViewEvent.class).toSnapshot();
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public Optional<PresenterViewSnapshot> clearPresentation(
        UUID sessionId,
        UUID presentationId,
        Instant updatedAt,
        PresenterViewClearReason reason
    ) {
        try {
            String result = redisTemplate.execute(
                CLEAR_PRESENTATION_SCRIPT,
                List.of(key(sessionId)),
                presentationId.toString(),
                updatedAt.toString(),
                Long.toString(ttl.toMillis()),
                channel,
                sessionId.toString(),
                reason.name()
            );
            if (!StringUtils.hasText(result)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(result, PresenterViewEvent.class).toSnapshot());
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public void clearSession(UUID sessionId, Instant updatedAt, PresenterViewClearReason reason) {
        try {
            String result = redisTemplate.execute(
                CLEAR_SESSION_SCRIPT,
                List.of(key(sessionId)),
                updatedAt.toString(),
                channel,
                sessionId.toString(),
                reason.name()
            );
            if (!StringUtils.hasText(result)) {
                throw new IllegalStateException("Redis clear-session script returned no result");
            }
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    private PresenterViewUnavailableException unavailable(Exception cause) {
        return new PresenterViewUnavailableException(cause);
    }

    // 프리픽스를 앞에 붙여줌
    private String key(UUID sessionId) {
        return KEY_PREFIX + sessionId;
    }

    private UUID parseUuid(Object value) {
        return value == null ? null : UUID.fromString(value.toString());
    }

    private Integer parseInteger(Object value) {
        return value == null ? null : Integer.valueOf(value.toString());
    }
}
