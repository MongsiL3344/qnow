package io.github.mongsil3344.qnow.bff.infrastructure.web;

import io.github.mongsil3344.qnow.bff.application.GetSessionPresentationListService;
import io.github.mongsil3344.qnow.bff.infrastructure.web.dto.SessionPresentationListResponse;
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

@Tag(name = "세션 대시보드", description = "세션 화면 데이터 조회 API")
@AllArgsConstructor
@RequestMapping("/organizations/{organizationId}/sessions/{sessionId}/presentations")
@RestController
public class SessionPresentationController {

    private final GetSessionPresentationListService getSessionPresentationListService;

    @Operation(summary = "세션 발표 자료 목록 조회 API")
    @GetMapping
    public ResponseEntity<SessionPresentationListResponse> getSessionPresentations(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID organizationId,
        @PathVariable UUID sessionId
    ) {
        return ResponseEntity.ok(
            SessionPresentationListResponse.from(
                getSessionPresentationListService.getSessionPresentations(
                    organizationId,
                    sessionId,
                    principal.id()
                )
            )
        );
    }
}
