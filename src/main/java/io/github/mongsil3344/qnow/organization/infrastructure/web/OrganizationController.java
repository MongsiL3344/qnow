package io.github.mongsil3344.qnow.organization.infrastructure.web;

import io.github.mongsil3344.qnow.organization.application.CreateOrganizationService;
import io.github.mongsil3344.qnow.organization.application.GetOrganizationListService;
import io.github.mongsil3344.qnow.organization.application.JoinOrganizationService;
import io.github.mongsil3344.qnow.organization.infrastructure.web.dto.CreateOrganizationRequest;
import io.github.mongsil3344.qnow.organization.infrastructure.web.dto.JoinOrganizationRequest;
import io.github.mongsil3344.qnow.organization.infrastructure.web.dto.OrganizationSearchPageResponse;
import io.github.mongsil3344.qnow.organization.infrastructure.web.dto.OrganizationSummaryResponse;
import io.github.mongsil3344.qnow.user.api.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "조직", description = "조직 생성, 조회, 검색, 참여 API")
@AllArgsConstructor
@RestController
public class OrganizationController {

    private final CreateOrganizationService createOrganizationService;
    private final JoinOrganizationService joinOrganizationService;
    private final GetOrganizationListService getOrganizationListService;

    @Operation(summary = "조직 개설 API")
    @PostMapping("/organizations")
    public ResponseEntity<Void> createOrganization(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateOrganizationRequest createOrganizationRequest
    ) {
        createOrganizationService.createOrganization(
            principal.id(),
            createOrganizationRequest.name(),
            createOrganizationRequest.detail(),
            createOrganizationRequest.password()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .build();
    }

    @Operation(summary = "내 조직 목록 조회 API")
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

    @Operation(summary = "조직 검색 API")
    @GetMapping("/organizations/search")
    public ResponseEntity<OrganizationSearchPageResponse> searchOrganizations(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam String keyword,
        @RequestParam int page,
        @RequestParam int size
    ) {
        return ResponseEntity.ok(
            OrganizationSearchPageResponse.from(
                getOrganizationListService.searchOrganizations(
                    principal.id(),
                    keyword,
                    PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                    )
                )
            )
        );
    }

    @Operation(summary = "조직 참여 API")
    @PostMapping("/organizations/{organizationId}/members")
    public ResponseEntity<Void> joinOrganization(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID organizationId,
        @Valid @RequestBody JoinOrganizationRequest joinOrganizationRequest
    ) {
        joinOrganizationService.joinOrganization(
            organizationId,
            principal.id(),
            joinOrganizationRequest.password()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .build();
    }
}
