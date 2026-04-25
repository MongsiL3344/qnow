package io.github.mongsil3344.qnow.organization.infrastructure.repo;

import io.github.mongsil3344.qnow.organization.domain.UserGroup;
import io.github.mongsil3344.qnow.organization.domain.UserGroupRole;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGroupRepository extends JpaRepository<UserGroup, UUID> {
    boolean existsByUserIdAndOrganizationIdAndDeletedAtIsNull(UUID userId, UUID organizationId);

    boolean existsByUserIdAndOrganizationIdAndRoleAndDeletedAtIsNull(UUID userId, UUID organizationId, UserGroupRole role);
}
