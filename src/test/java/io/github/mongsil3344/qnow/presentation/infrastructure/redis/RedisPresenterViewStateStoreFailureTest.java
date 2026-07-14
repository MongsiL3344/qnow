package io.github.mongsil3344.qnow.presentation.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.github.mongsil3344.qnow.presentation.application.PresenterViewMetrics;
import io.github.mongsil3344.qnow.presentation.application.exception.PresenterViewUnavailableException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class RedisPresenterViewStateStoreFailureTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Test
    void redisConnectionFailureIsConvertedToUnavailableAndRecorded() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RedisPresenterViewStateStore stateStore = new RedisPresenterViewStateStore(
            redisTemplate,
            JsonMapper.builder().findAndAddModules().build(),
            new PresenterViewMetrics(meterRegistry),
            Duration.ofHours(24),
            "qnow:test:presenter-view:events"
        );
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.multiGet(anyString(), anyList()))
            .thenThrow(new RedisConnectionFailureException("Redis is unavailable"));

        assertThatThrownBy(() -> stateStore.get(UUID.randomUUID()))
            .isInstanceOf(PresenterViewUnavailableException.class)
            .hasCauseInstanceOf(RedisConnectionFailureException.class);

        assertThat(meterRegistry.counter("qnow.presenter.view.redis.failure").count()).isEqualTo(1);
    }
}
