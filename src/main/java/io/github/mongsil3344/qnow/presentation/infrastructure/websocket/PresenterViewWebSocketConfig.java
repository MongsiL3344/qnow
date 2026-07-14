package io.github.mongsil3344.qnow.presentation.infrastructure.websocket;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// 웹소켓 설정 클래스
@Configuration
@EnableWebSocketMessageBroker
public class PresenterViewWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final PresenterViewSubscriptionInterceptor subscriptionInterceptor;
    private final String[] allowedOrigins;

    public PresenterViewWebSocketConfig(
        PresenterViewSubscriptionInterceptor subscriptionInterceptor,
        @Value("${qnow.websocket.allowed-origins:http://localhost:3000}") String allowedOrigins
    ) {
        this.subscriptionInterceptor = subscriptionInterceptor;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toArray(String[]::new);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic")
            .setHeartbeatValue(new long[]{10_000, 10_000})
            .setTaskScheduler(presenterViewBrokerTaskScheduler());
        registry.setApplicationDestinationPrefixes("/app");
        registry.setPreservePublishOrder(true);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(subscriptionInterceptor);
    }

    @Bean
    ThreadPoolTaskScheduler presenterViewBrokerTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("presenter-view-broker-");
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}
