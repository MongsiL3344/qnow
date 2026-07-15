package io.github.mongsil3344.qnow.session.infrastructure.web;

import io.github.mongsil3344.qnow.session.api.GuestPrincipal;
import io.github.mongsil3344.qnow.session.application.JoinGuestSessionService;
import io.github.mongsil3344.qnow.session.application.dto.JoinGuestSessionResult;
import io.github.mongsil3344.qnow.session.infrastructure.web.dto.GuestSessionParticipationRequest;
import io.github.mongsil3344.qnow.session.infrastructure.web.dto.GuestSessionParticipationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "발표 세션", description = "발표 세션 생성과 참여 API")
@AllArgsConstructor
@RequestMapping("/guest/session-participations")
@RestController
public class GuestSessionParticipationController {

    private static final SimpleGrantedAuthority GUEST_AUTHORITY = new SimpleGrantedAuthority("ROLE_GUEST");

    private final JoinGuestSessionService joinGuestSessionService;
    private final SecurityContextRepository securityContextRepository;

    @Operation(summary = "비회원 세션 참여 API")
    @PostMapping
    public ResponseEntity<GuestSessionParticipationResponse> join(
        @Valid @RequestBody GuestSessionParticipationRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        JoinGuestSessionResult result = joinGuestSessionService.join(request.code(), request.nickname());

        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }

        GuestPrincipal principal = new GuestPrincipal(result.participantId(), result.sessionId());
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            List.of(GUEST_AUTHORITY)
        );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(GuestSessionParticipationResponse.from(result));
    }
}
