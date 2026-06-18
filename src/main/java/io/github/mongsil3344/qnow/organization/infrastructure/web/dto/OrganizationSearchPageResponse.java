package io.github.mongsil3344.qnow.organization.infrastructure.web.dto;

import io.github.mongsil3344.qnow.organization.application.dto.OrganizationSearchPageResult;
import java.util.List;

public record OrganizationSearchPageResponse(
    List<OrganizationSearchResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {

    public static OrganizationSearchPageResponse from(OrganizationSearchPageResult result) {
        return new OrganizationSearchPageResponse(
            result.content().stream()
                .map(OrganizationSearchResponse::from)
                .toList(),
            result.page(),
            result.size(),
            result.totalElements(),
            result.totalPages(),
            result.first(),
            result.last()
        );
    }
}
