package io.github.mongsil3344.qnow.organization.application;

import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.organization.api.OrganizationInfo;
import io.github.mongsil3344.qnow.organization.application.exception.OrganizationMemberRequiredException;
import io.github.mongsil3344.qnow.organization.application.exception.OrganizationNotFoundException;
import io.github.mongsil3344.qnow.organization.domain.Organization;
import io.github.mongsil3344.qnow.organization.domain.UserGroup;
import io.github.mongsil3344.qnow.organization.domain.UserGroupRole;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.OrganizationRepository;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.UserGroupRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Component
public class OrganizationQueryApiImpl implements OrganizationQueryApi {

    private final OrganizationRepository organizationRepository;
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

    @Override
    @Transactional(readOnly = true)
    public OrganizationInfo getOrganizationInfo(UUID organizationId, UUID userId) {
        Organization organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
            .orElseThrow(OrganizationNotFoundException::new);

        if (!existsUserInOrganization(userId, organizationId)) {
            throw new OrganizationMemberRequiredException();
        }

        long memberCount = userGroupRepository.countByOrganizationIdAndDeletedAtIsNull(organizationId);

        return new OrganizationInfo(
            organization.getId(),
            organization.getName(),
            organization.getDetail(),
            memberCount
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationInfo> findOrganizationInfosByUserId(UUID userId) {
        List<UserGroup> userGroups = userGroupRepository.findAllActiveByUserIdWithOrganization(userId);

        if (userGroups.isEmpty()) {
            return List.of();
        }

        Map<UUID, Long> memberCounts = getMemberCounts(userGroups);

        return userGroups.stream()
            .map(userGroup -> {
                Organization organization = userGroup.getOrganization();

                return new OrganizationInfo(
                    organization.getId(),
                    organization.getName(),
                    organization.getDetail(),
                    memberCounts.getOrDefault(organization.getId(), 0L)
                );
            })
            .toList();
    }

    private Map<UUID, Long> getMemberCounts(List<UserGroup> userGroups) {
        List<UUID> organizationIds = userGroups.stream()
            .map(userGroup -> userGroup.getOrganization().getId())
            .toList();

        return userGroupRepository.countMembersByOrganizationIds(organizationIds).stream()
            .collect(Collectors.toMap(
                UserGroupRepository.OrganizationMemberCount::getOrganizationId,
                UserGroupRepository.OrganizationMemberCount::getMemberCount
            ));
    }
}
