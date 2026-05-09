package io.github.mongsil3344.qnow.organization.infrastructure.web;

import io.github.mongsil3344.qnow.organization.application.CreateOrganizationService;
import io.github.mongsil3344.qnow.organization.application.JoinOrganizationService;
import io.github.mongsil3344.qnow.organization.infrastructure.web.dto.CreateOrganizationRequest;
import io.github.mongsil3344.qnow.organization.infrastructure.web.dto.JoinOrganizationRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
public class OrganizationController {

    private final CreateOrganizationService createOrganizationService;
    private final JoinOrganizationService joinOrganizationService;

    // 조직 개설 API
    @PostMapping("/organizations")
    public ResponseEntity<Void> createOrganization (@Valid @RequestBody CreateOrganizationRequest createOrganizationRequest) {
        createOrganizationService.createOrganization(
            createOrganizationRequest.userId(),
            createOrganizationRequest.name(),
            createOrganizationRequest.detail(),
            createOrganizationRequest.password()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .build();
    }

    // 조직 참여 API
    @PostMapping("/organizations/{organizationId}/members")
    public ResponseEntity<Void> joinOrganization(
        @PathVariable UUID organizationId,
        @Valid @RequestBody JoinOrganizationRequest joinOrganizationRequest
    ) {
        // TODO: 나중에 인증정보에서 가져오도록 수정해야함
        joinOrganizationService.joinOrganization(
            organizationId,
            joinOrganizationRequest.userId(),
            joinOrganizationRequest.password()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .build();
    }
}
