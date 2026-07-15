package io.github.mongsil3344.qnow.session.infrastructure.security;

import io.github.mongsil3344.qnow.session.api.GuestPrincipal;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.session.api.SessionActorResolver;
import io.github.mongsil3344.qnow.user.api.UserPrincipal;
import java.security.Principal;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class DefaultSessionActorResolver implements SessionActorResolver {

    @Override
    public Optional<SessionActor> resolve(Principal principal) {
        Object authenticationPrincipal = principal instanceof Authentication authentication
            ? authentication.getPrincipal()
            : principal;

        return switch (authenticationPrincipal) {
            case UserPrincipal userPrincipal -> Optional.of(
                new SessionActor.Member(userPrincipal.id())
            );
            case GuestPrincipal guestPrincipal -> Optional.of(
                new SessionActor.Guest(
                    guestPrincipal.participantId(),
                    guestPrincipal.sessionId()
                )
            );
            case null, default -> Optional.empty();
        };
    }
}
