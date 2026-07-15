package io.github.mongsil3344.qnow.presentation.infrastructure.web;

import io.github.mongsil3344.qnow.presentation.application.GetPresenterViewService;
import io.github.mongsil3344.qnow.presentation.application.UpdatePresenterViewService;
import io.github.mongsil3344.qnow.presentation.application.dto.PresenterViewResult;
import io.github.mongsil3344.qnow.presentation.infrastructure.web.dto.PresenterViewResponse;
import io.github.mongsil3344.qnow.presentation.infrastructure.web.dto.UpdatePresenterViewRequest;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.user.api.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "발표자 화면", description = "진행 중인 세션의 발표 자료와 페이지 동기화 API")
@AllArgsConstructor
@RequestMapping("/organizations/{organizationId}/sessions/{sessionId}/presenter-view")
@RestController
public class PresenterViewController {

    private final GetPresenterViewService getPresenterViewService;
    private final UpdatePresenterViewService updatePresenterViewService;

    @Operation(summary = "현재 발표자 화면 조회 API")
    @GetMapping
    public ResponseEntity<PresenterViewResponse> getPresenterView(
        @Parameter(hidden = true) SessionActor actor,
        @PathVariable UUID organizationId,
        @PathVariable UUID sessionId
    ) {
        PresenterViewResult result = getPresenterViewService.getPresenterView(
            organizationId,
            sessionId,
            actor
        );

        // 레디스 스냅샷 정보 반환, 스냅샷의 값이 비워진 상태면 발표자가 보고있는 슬라이드가 없는 것임
        return ResponseEntity.ok(PresenterViewResponse.from(result));
    }

    @Operation(summary = "발표자 화면 변경 API")
    @PutMapping
    public ResponseEntity<PresenterViewResponse> updatePresenterView(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID organizationId,
        @PathVariable UUID sessionId,
        @Valid @RequestBody UpdatePresenterViewRequest request
    ) {
        PresenterViewResult result = updatePresenterViewService.updatePresenterView(
            organizationId,
            sessionId,
            principal.id(),
            request.presentationId(),
            request.pageNumber()
        );
        return ResponseEntity.ok(PresenterViewResponse.from(result));
    }
}
