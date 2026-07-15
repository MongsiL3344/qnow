package io.github.mongsil3344.qnow.session.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mongsil3344.qnow.session.api.GuestPrincipal;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.user.api.UserPrincipal;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class DefaultSessionActorResolverTest {

    private final DefaultSessionActorResolver resolver = new DefaultSessionActorResolver();

    @Test
    void 회원_인증을_회원_세션_행위자로_변환한다() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
            userId,
            "member@example.com",
            "member",
            "encoded-password"
        );
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
            principal,
            principal.getPassword(),
            principal.getAuthorities()
        );

        assertThat(resolver.resolve(authentication))
            .contains(new SessionActor.Member(userId));
    }

    @Test
    void 비회원_인증을_비회원_세션_행위자로_변환한다() {
        UUID participantId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        GuestPrincipal principal = new GuestPrincipal(participantId, sessionId);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            List.of()
        );

        assertThat(resolver.resolve(authentication))
            .contains(new SessionActor.Guest(participantId, sessionId));
    }

    @Test
    void 지원하지_않는_인증_주체는_변환하지_않는다() {
        Principal principal = () -> "anonymous";

        assertThat(resolver.resolve(principal)).isEmpty();
        assertThat(resolver.resolve(null)).isEmpty();
    }
}
