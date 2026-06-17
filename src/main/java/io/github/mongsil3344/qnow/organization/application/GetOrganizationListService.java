package io.github.mongsil3344.qnow.organization.application;

import io.github.mongsil3344.qnow.organization.api.OrganizationInfo;
import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.organization.application.dto.OrganizationSummaryResult;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class GetOrganizationListService {

    private final OrganizationQueryApi organizationQueryApi;

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
}
