package io.github.mongsil3344.qnow.session.infrastructure.web;

import io.github.mongsil3344.qnow.session.application.CreateSessionService;
import io.github.mongsil3344.qnow.session.application.JoinSessionService;
import io.github.mongsil3344.qnow.session.infrastructure.web.dto.CreateSessionRequest;
import io.github.mongsil3344.qnow.session.infrastructure.web.dto.JoinSessionRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RequestMapping("/organizations")
@RestController
public class SessionController {

    private final CreateSessionService createSessionService;
    private final JoinSessionService joinSessionService;

    // 세션 생성 API
    @PostMapping("/{organizationId}/sessions")
    public ResponseEntity<Void> createSession(@PathVariable UUID organizationId, @Valid @RequestBody CreateSessionRequest createSessionRequest) {
        // TODO: 나중에 인증정보에서 가져오도록 수정해야함
        createSessionService.createSession(organizationId, createSessionRequest.creatorId(), createSessionRequest.title());

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .build();
    }

    // 세션 참여 API
    @PostMapping("/{organizationId}/sessions/{sessionId}/participants")
    public ResponseEntity<Void> joinSession(
        @PathVariable UUID organizationId,
        @PathVariable UUID sessionId,
        @Valid @RequestBody JoinSessionRequest joinSessionRequest
    ) {
        // TODO: 나중에 인증정보에서 가져오도록 수정해야함
        joinSessionService.joinSession(organizationId, sessionId, joinSessionRequest.userId());

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .build();
    }
}
