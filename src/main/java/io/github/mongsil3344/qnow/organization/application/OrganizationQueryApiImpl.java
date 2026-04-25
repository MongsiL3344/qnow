package io.github.mongsil3344.qnow.organization.application;

import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.organization.domain.UserGroupRole;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.UserGroupRepository;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class OrganizationQueryApiImpl implements OrganizationQueryApi {

    private final UserGroupRepository userGroupRepository;

    @Override
    public boolean existsUserInOrganization(UUID userId, UUID organizationId) {
        return userGroupRepository.existsByUserIdAndOrganizationIdAndDeletedAtIsNull(userId, organizationId);
    }

    @Override
    public boolean isAdminInOrganization(UUID userId, UUID organizationId) {
        return userGroupRepository.existsByUserIdAndOrganizationIdAndRoleAndDeletedAtIsNull(
                userId,
                organizationId,
                UserGroupRole.ADMIN
        );
    }
}
