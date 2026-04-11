package io.github.mongsil3344.qnow.organization.infrastructure.web;

import io.github.mongsil3344.qnow.organization.application.CreateOrganizationService;
import io.github.mongsil3344.qnow.organization.infrastructure.web.dto.CreateOrganizationRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
public class OrganizationController {

    private final CreateOrganizationService createOrganizationService;

    @PostMapping("/organization")
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
}
