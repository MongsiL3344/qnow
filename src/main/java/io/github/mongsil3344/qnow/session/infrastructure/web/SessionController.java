package io.github.mongsil3344.qnow.session.infrastructure.web;

import io.github.mongsil3344.qnow.session.application.CreateSessionService;
import io.github.mongsil3344.qnow.session.application.EndSessionService;
import io.github.mongsil3344.qnow.session.application.ExitSessionService;
import io.github.mongsil3344.qnow.session.application.GetSessionParticipateCodeService;
import io.github.mongsil3344.qnow.session.application.JoinSessionService;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.session.infrastructure.web.dto.CreateSessionRequest;
import io.github.mongsil3344.qnow.session.infrastructure.web.dto.SessionParticipateCodeResponse;
import io.github.mongsil3344.qnow.user.api.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "발표 세션", description = "발표 세션 생성과 참여 API")
@AllArgsConstructor
@RequestMapping("/organizations")
@RestController
public class SessionController {

    private final CreateSessionService createSessionService;
    private final EndSessionService endSessionService;
    private final JoinSessionService joinSessionService;
    private final ExitSessionService exitSessionService;
    private final GetSessionParticipateCodeService getSessionParticipateCodeService;

    @Operation(summary = "세션 생성 API")
    @PostMapping("/{organizationId}/sessions")
    public ResponseEntity<Void> createSession(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID organizationId,
        @Valid @RequestBody CreateSessionRequest createSessionRequest
    ) {
        createSessionService.createSession(
            organizationId,
            principal.id(),
            createSessionRequest.title(),
            createSessionRequest.startAt(),
            createSessionRequest.guestUpvoteAllowed()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .build();
    }

    @Operation(summary = "세션 참여 API")
    @PostMapping("/{organizationId}/sessions/{sessionId}/participants")
    public ResponseEntity<Void> joinSession(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID organizationId,
        @PathVariable UUID sessionId
    ) {
        joinSessionService.joinSession(organizationId, sessionId, principal.id());

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .build();
    }

    @Operation(summary = "세션 비회원 참가 코드 조회 API")
    @GetMapping("/{organizationId}/sessions/{sessionId}/participation-code")
    public ResponseEntity<SessionParticipateCodeResponse> getParticipateCode(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID organizationId,
        @PathVariable UUID sessionId
    ) {
        return ResponseEntity.ok(
            SessionParticipateCodeResponse.from(
                getSessionParticipateCodeService.getParticipateCode(
                    organizationId,
                    sessionId,
                    principal.id()
                )
            )
        );
    }

    @Operation(summary = "세션 퇴장 API")
    @PostMapping("/{organizationId}/sessions/{sessionId}/participants/exit")
    public ResponseEntity<Void> exitSession(
        @Parameter(hidden = true) SessionActor actor,
        @PathVariable UUID organizationId,
        @PathVariable UUID sessionId,
        HttpServletRequest request
    ) {
        exitSessionService.exitSession(
            organizationId,
            sessionId,
            actor
        );

        if (actor instanceof SessionActor.Guest) {
            SecurityContextHolder.clearContext();
            HttpSession httpSession = request.getSession(false);
            if (httpSession != null) {
                httpSession.invalidate();
            }
        }

        return ResponseEntity
            .noContent()
            .build();
    }

    @Operation(summary = "세션 종료 API")
    @PostMapping("/{organizationId}/sessions/{sessionId}/end")
    public ResponseEntity<Void> endSession(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID organizationId,
        @PathVariable UUID sessionId
    ) {
        endSessionService.endSession(organizationId, sessionId, principal.id());

        return ResponseEntity
            .noContent()
            .build();
    }
}
