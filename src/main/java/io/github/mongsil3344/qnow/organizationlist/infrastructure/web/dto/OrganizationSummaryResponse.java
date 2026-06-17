package io.github.mongsil3344.qnow.organizationlist.infrastructure.web.dto;

import io.github.mongsil3344.qnow.organizationlist.application.dto.OrganizationSummaryResult;
import java.util.UUID;

public record OrganizationSummaryResponse(
    UUID id,
    String name,
    String detail,
    long memberCount
) {

    public static OrganizationSummaryResponse from(OrganizationSummaryResult result) {
        return new OrganizationSummaryResponse(
            result.id(),
            result.name(),
            result.detail(),
            result.memberCount()
        );
    }
}
