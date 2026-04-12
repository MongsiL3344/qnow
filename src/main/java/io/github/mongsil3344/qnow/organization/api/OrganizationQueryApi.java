package io.github.mongsil3344.qnow.organization.api;

import java.util.UUID;

public interface OrganizationQueryApi {
    boolean existsUserInOrganization(UUID userId, UUID organizationId);

    boolean isAdminInOrganization(UUID userId, UUID organizationId);
}
