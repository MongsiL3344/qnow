package io.github.mongsil3344.qnow.organizationlist.infrastructure.web;

import io.github.mongsil3344.qnow.organizationlist.application.GetOrganizationListService;
import io.github.mongsil3344.qnow.organizationlist.infrastructure.web.dto.OrganizationSummaryResponse;
import io.github.mongsil3344.qnow.user.api.UserPrincipal;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
public class OrganizationListController {

    private final GetOrganizationListService getOrganizationListService;

    // 내 조직 목록 조회 API
    @GetMapping("/organizations")
    public ResponseEntity<List<OrganizationSummaryResponse>> getOrganizations(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
            getOrganizationListService.getOrganizations(principal.id()).stream()
                .map(OrganizationSummaryResponse::from)
                .toList()
        );
    }
}
