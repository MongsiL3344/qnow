package io.github.mongsil3344.qnow.organizationdetail.infrastructure.web;

import io.github.mongsil3344.qnow.organizationdetail.application.GetOrganizationDetailService;
import io.github.mongsil3344.qnow.organizationdetail.infrastructure.web.dto.OrganizationDetailResponse;
import io.github.mongsil3344.qnow.user.api.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "조직", description = "조직 생성, 조회, 검색, 참여 API")
@AllArgsConstructor
@RequestMapping("/organizations")
@RestController
public class OrganizationDetailController {

    private final GetOrganizationDetailService getOrganizationDetailService;

    @Operation(summary = "조직 상세 조회 API")
    @GetMapping("/{organizationId}")
    public ResponseEntity<OrganizationDetailResponse> getOrganizationDetail(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID organizationId
    ) {
        return ResponseEntity.ok(
            OrganizationDetailResponse.from(
                getOrganizationDetailService.getOrganizationDetail(organizationId, principal.id())
            )
        );
    }
}
