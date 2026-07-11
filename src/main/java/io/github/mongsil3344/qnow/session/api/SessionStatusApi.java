package io.github.mongsil3344.qnow.session.api;

import java.util.UUID;

public interface SessionStatusApi {

    void requireNotEnded(UUID sessionId);
}
