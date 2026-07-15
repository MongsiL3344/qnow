package io.github.mongsil3344.qnow.session.infrastructure.web;

import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.session.api.SessionActorResolver;
import java.security.Principal;
import lombok.AllArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@AllArgsConstructor
@Component
class SessionActorMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private final SessionActorResolver sessionActorResolver;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == SessionActor.class;
    }

    @Override
    public SessionActor resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {
        Principal principal = webRequest.getUserPrincipal();
        return sessionActorResolver.resolve(principal)
            .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                "세션 요청자를 식별할 수 없습니다"
            ));
    }
}
