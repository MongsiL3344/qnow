package io.github.mongsil3344.qnow.organization.api;

import java.util.List;
import java.util.UUID;

public interface OrganizationQueryApi {
    boolean existsUserInOrganization(UUID userId, UUID organizationId);

    boolean isAdminInOrganization(UUID userId, UUID organizationId);

    OrganizationInfo getOrganizationInfo(UUID organizationId, UUID userId);

    List<OrganizationInfo> findOrganizationInfosByUserId(UUID userId);
}
