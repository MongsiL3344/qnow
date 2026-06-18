package io.github.mongsil3344.qnow.organization.application.dto;

import java.util.List;

public record OrganizationSearchPageResult(
    List<OrganizationSearchResult> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
}
