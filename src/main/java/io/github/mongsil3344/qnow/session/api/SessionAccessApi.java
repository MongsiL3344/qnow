package io.github.mongsil3344.qnow.session.api;

import java.util.UUID;

public interface SessionAccessApi {

    boolean isSessionCreator(UUID sessionId, UUID userId);
}
