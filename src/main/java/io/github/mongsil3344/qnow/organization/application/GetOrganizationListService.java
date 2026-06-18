package io.github.mongsil3344.qnow.organization.application;

import io.github.mongsil3344.qnow.organization.api.OrganizationInfo;
import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.organization.application.dto.OrganizationSearchPageResult;
import io.github.mongsil3344.qnow.organization.application.dto.OrganizationSearchResult;
import io.github.mongsil3344.qnow.organization.application.dto.OrganizationSummaryResult;
import io.github.mongsil3344.qnow.organization.application.exception.InvalidOrganizationSearchKeywordException;
import io.github.mongsil3344.qnow.organization.domain.Organization;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.OrganizationRepository;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.UserGroupRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class GetOrganizationListService {

    private final OrganizationQueryApi organizationQueryApi;
    private final OrganizationRepository organizationRepository;
    private final UserGroupRepository userGroupRepository;

    @Transactional(readOnly = true)
    public List<OrganizationSummaryResult> getOrganizations(UUID userId) {
        List<OrganizationInfo> organizations = organizationQueryApi.findOrganizationInfosByUserId(userId);

        if (organizations.isEmpty()) {
            return List.of();
        }

        return organizations.stream()
            .map(organization -> new OrganizationSummaryResult(
                organization.id(),
                organization.name(),
                organization.detail(),
                organization.memberCount()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationSearchPageResult searchOrganizations(UUID userId, String keyword, Pageable pageable) {
        String trimmedKeyword = keyword.trim();

        if (trimmedKeyword.isEmpty()) {
            throw new InvalidOrganizationSearchKeywordException();
        }

        Page<Organization> organizationPage =
            organizationRepository.findAllByDeletedAtIsNullAndNameContainingIgnoreCase(trimmedKeyword, pageable);
        List<Organization> organizations = organizationPage.getContent();

        if (organizations.isEmpty()) {
            return toSearchPageResult(organizationPage, List.of());
        }

        List<UUID> organizationIds = organizations.stream()
            .map(Organization::getId)
            .toList();
        Map<UUID, Long> memberCounts = getMemberCounts(organizationIds);
        Set<UUID> joinedOrganizationIds = userGroupRepository.findActiveOrganizationIdsByUserIdAndOrganizationIds(
            userId,
            organizationIds
        );
        List<OrganizationSearchResult> content = organizations.stream()
            .map(organization -> new OrganizationSearchResult(
                organization.getId(),
                organization.getName(),
                organization.getDetail(),
                memberCounts.getOrDefault(organization.getId(), 0L),
                organization.getPassword() != null,
                joinedOrganizationIds.contains(organization.getId())
            ))
            .toList();

        return toSearchPageResult(organizationPage, content);
    }

    private Map<UUID, Long> getMemberCounts(List<UUID> organizationIds) {
        return userGroupRepository.countMembersByOrganizationIds(organizationIds).stream()
            .collect(Collectors.toMap(
                UserGroupRepository.OrganizationMemberCount::getOrganizationId,
                UserGroupRepository.OrganizationMemberCount::getMemberCount
            ));
    }

    private OrganizationSearchPageResult toSearchPageResult(
        Page<Organization> organizationPage,
        List<OrganizationSearchResult> content
    ) {
        return new OrganizationSearchPageResult(
            content,
            organizationPage.getNumber(),
            organizationPage.getSize(),
            organizationPage.getTotalElements(),
            organizationPage.getTotalPages(),
            organizationPage.isFirst(),
            organizationPage.isLast()
        );
    }
}
