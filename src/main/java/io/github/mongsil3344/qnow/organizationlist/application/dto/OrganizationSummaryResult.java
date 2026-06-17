package io.github.mongsil3344.qnow.organizationlist.application.dto;

import java.util.UUID;

public record OrganizationSummaryResult(
    UUID id,
    String name,
    String detail,
    long memberCount
) {
}
