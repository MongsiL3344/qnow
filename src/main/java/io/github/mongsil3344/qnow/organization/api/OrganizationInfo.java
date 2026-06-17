package io.github.mongsil3344.qnow.organization.api;

import java.util.UUID;

public record OrganizationInfo(
    UUID id,
    String name,
    String detail,
    long memberCount
) {
}
