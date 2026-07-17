package io.github.mongsil3344.qnow.bff.infrastructure.web;

import io.github.mongsil3344.qnow.bff.application.GetSessionScreenService;
import io.github.mongsil3344.qnow.bff.infrastructure.web.dto.SessionScreenResponse;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "세션 대시보드", description = "세션 화면 데이터 조회 API")
@AllArgsConstructor
@RequestMapping("/organizations/{organizationId}/sessions/{sessionId}")
@RestController
public class SessionScreenController {

    private final GetSessionScreenService getSessionScreenService;

    @Operation(summary = "세션 화면 조회 API")
    @GetMapping
    public ResponseEntity<SessionScreenResponse> getSessionScreen(
        @Parameter(hidden = true) SessionActor actor,
        @PathVariable UUID organizationId,
        @PathVariable UUID sessionId
    ) {
        return ResponseEntity.ok(
            SessionScreenResponse.from(
                getSessionScreenService.getSessionScreen(organizationId, sessionId, actor)
            )
        );
    }
}
