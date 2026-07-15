package io.github.mongsil3344.qnow.session.infrastructure.web;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AllArgsConstructor
@Configuration
class SessionWebMvcConfiguration implements WebMvcConfigurer {

    private final SessionActorMethodArgumentResolver sessionActorMethodArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(sessionActorMethodArgumentResolver);
    }
}
