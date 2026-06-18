package io.github.mongsil3344.qnow.organization.application.dto;

import java.util.UUID;

public record OrganizationSearchResult(
    UUID id,
    String name,
    String detail,
    long memberCount,
    boolean requiresPassword,
    boolean joined
) {
}
