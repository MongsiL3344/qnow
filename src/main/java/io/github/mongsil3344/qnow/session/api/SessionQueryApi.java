package io.github.mongsil3344.qnow.session.api;

import java.util.List;
import java.util.UUID;

public interface SessionQueryApi {

    List<SessionSummary> findSessionSummariesByOrganizationId(UUID organizationId);

    boolean existsSessionInOrganization(UUID sessionId, UUID organizationId);
}
