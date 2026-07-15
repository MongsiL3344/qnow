package io.github.mongsil3344.qnow.presentation.infrastructure.redis;

import io.github.mongsil3344.qnow.presentation.application.PresenterViewMetrics;
import io.github.mongsil3344.qnow.presentation.application.PresenterViewStateStore;
import io.github.mongsil3344.qnow.presentation.application.PresenterViewUpdateResult;
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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
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

    private static final DefaultRedisScript<String> UPDATE_SCRIPT = loadScript(
        "redis/scripts/presenter-view/update.lua"
    );
    private static final DefaultRedisScript<String> CLEAR_PRESENTATION_SCRIPT = loadScript(
        "redis/scripts/presenter-view/clear-presentation.lua"
    );
    private static final DefaultRedisScript<String> CLEAR_SESSION_SCRIPT = loadScript(
        "redis/scripts/presenter-view/clear-session.lua"
    );

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PresenterViewMetrics metrics;
    private final Duration ttl;
    private final String channel;

    public RedisPresenterViewStateStore(
        StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper,
        PresenterViewMetrics metrics,
        @Value("${qnow.presenter-view.ttl:24h}") Duration ttl,
        @Value("${qnow.presenter-view.channel:qnow:presenter-view:events}") String channel
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
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
    public PresenterViewUpdateResult update(
        UUID sessionId,
        UUID presentationId,
        int pageNumber,
        Instant updatedAt
    ) {
        try {
            // 루아 스크립트 실행해서 결과(변경여부, event 객체)를 String으로 받아옴
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
            // 받아온 String 결과를 Java 객체로 역직렬화
            JsonNode root = objectMapper.readTree(result);

            // 역직렬화 된 객체에서 event객체만 따로 뽑음
            PresenterViewEvent event = objectMapper.treeToValue(root.get("event"), PresenterViewEvent.class);

            // DTO 객체로 반환
            return new PresenterViewUpdateResult(event.toSnapshot(), root.path("changed").asBoolean());
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
        metrics.recordRedisFailure();
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

    private static DefaultRedisScript<String> loadScript(String location) {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(location));
        script.setResultType(String.class);
        return script;
    }
}
