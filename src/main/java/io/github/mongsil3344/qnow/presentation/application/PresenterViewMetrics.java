package io.github.mongsil3344.qnow.presentation.application;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

// Redis 로그 집계용 클래스
@Component
public class PresenterViewMetrics {

    private final Counter updateSuccess;
    private final Counter updateFailure;
    private final Counter redisFailure;
    private final Counter subscriptionDenied;
    private final Set<String> websocketSessions = ConcurrentHashMap.newKeySet();

    public PresenterViewMetrics(MeterRegistry meterRegistry) {
        updateSuccess = meterRegistry.counter("qnow.presenter.view.update.success");
        updateFailure = meterRegistry.counter("qnow.presenter.view.update.failure");
        redisFailure = meterRegistry.counter("qnow.presenter.view.redis.failure");
        subscriptionDenied = meterRegistry.counter("qnow.presenter.view.websocket.subscription.denied");
        Gauge.builder("qnow.presenter.view.websocket.connections", websocketSessions, Set::size)
            .register(meterRegistry);
    }

    public void recordUpdateSuccess() {
        updateSuccess.increment();
    }

    public void recordUpdateFailure() {
        updateFailure.increment();
    }

    public void recordRedisFailure() {
        redisFailure.increment();
    }

    public void recordSubscriptionDenied() {
        subscriptionDenied.increment();
    }

    public void connected(String sessionId) {
        websocketSessions.add(sessionId);
    }

    public void disconnected(String sessionId) {
        websocketSessions.remove(sessionId);
    }
}
