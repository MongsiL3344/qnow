package io.github.mongsil3344.qnow.presentation.infrastructure.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.messaging.web.csrf.CsrfChannelInterceptor;

// ws 보안규칙
@Configuration
@EnableWebSocketSecurity
public class PresenterViewWebSocketSecurityConfig {

    @Bean("csrfChannelInterceptor")
    ChannelInterceptor csrfChannelInterceptor() {
        return new CsrfChannelInterceptor();
    }

    @Bean
    AuthorizationManager<Message<?>> messageAuthorizationManager(
        MessageMatcherDelegatingAuthorizationManager.Builder messages
    ) {
        messages
            .nullDestMatcher().authenticated()
            .simpSubscribeDestMatchers("/topic/sessions/*/presenter-view").authenticated()
            .simpTypeMatchers(SimpMessageType.MESSAGE, SimpMessageType.SUBSCRIBE).denyAll()
            .anyMessage().authenticated();
        return messages.build();
    }
}
