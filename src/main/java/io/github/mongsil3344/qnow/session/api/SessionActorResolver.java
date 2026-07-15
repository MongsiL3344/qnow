package io.github.mongsil3344.qnow.session.api;

import java.security.Principal;
import java.util.Optional;

public interface SessionActorResolver {

    Optional<SessionActor> resolve(Principal principal);
}
