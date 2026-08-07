package io.github.mongsil3344.qnow.question.infrastructure.web;

import io.github.mongsil3344.qnow.question.application.ApproveControlRequestService;
import io.github.mongsil3344.qnow.question.application.CreateControlRequestService;
import io.github.mongsil3344.qnow.question.infrastructure.web.dto.CreateControlRequestRequest;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.user.api.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "질문", description = "발표 자료 질문 API")
@AllArgsConstructor
@RestController
public class ControlRequestController {

    private final CreateControlRequestService createControlRequestService;
    private final ApproveControlRequestService approveControlRequestService;

    @Operation(summary = "발표 제어 요청 등록 API")
    @PostMapping("/presentations/{presentationId}/questions/control-requests")
    public ResponseEntity<Void> createControlRequest(
            @Parameter(hidden = true) SessionActor actor,
            @PathVariable UUID presentationId,
            @Valid @RequestBody CreateControlRequestRequest request
    ) {
        createControlRequestService.createControlRequest(
                presentationId,
                actor,
                request.pageNumber()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @Operation(summary = "발표 제어 요청 승인 표시 API")
    @PutMapping("/questions/{questionId}/approval")
    public ResponseEntity<Void> approveControlRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID questionId
    ) {
        approveControlRequestService.approveControlRequest(questionId, principal.id());
        return ResponseEntity.noContent().build();
    }
}
