package io.github.mongsil3344.qnow.presentation.infrastructure.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@ConditionalOnProperty(
    prefix = "qnow.presenter-view",
    name = "realtime-enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class PresenterViewRedisConfig {

    @Bean
    RedisMessageListenerContainer presenterViewRedisMessageListenerContainer(
        RedisConnectionFactory connectionFactory,
        PresenterViewRedisMessageListener listener, // Redis 메세지를 처리할 리스너 구현체
        @Value("${qnow.presenter-view.channel:qnow:presenter-view:events}") String channel
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listener, new ChannelTopic(channel)); // 컨테이너가 구독할 채널, 이벤트를 받으면 호출할 리스너 설정
        return container;
    }
}
