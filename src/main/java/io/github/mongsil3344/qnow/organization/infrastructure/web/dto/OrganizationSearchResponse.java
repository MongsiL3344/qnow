package io.github.mongsil3344.qnow.organization.infrastructure.web.dto;

import io.github.mongsil3344.qnow.organization.application.dto.OrganizationSearchResult;
import java.util.UUID;

public record OrganizationSearchResponse(
    UUID id,
    String name,
    String detail,
    long memberCount,
    boolean requiresPassword,
    boolean joined
) {

    public static OrganizationSearchResponse from(OrganizationSearchResult result) {
        return new OrganizationSearchResponse(
            result.id(),
            result.name(),
            result.detail(),
            result.memberCount(),
            result.requiresPassword(),
            result.joined()
        );
    }
}
