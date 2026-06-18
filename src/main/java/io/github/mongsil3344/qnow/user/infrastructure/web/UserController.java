package io.github.mongsil3344.qnow.user.infrastructure.web;

import io.github.mongsil3344.qnow.user.application.SignUpService;
import io.github.mongsil3344.qnow.user.api.UserPrincipal;
import io.github.mongsil3344.qnow.user.infrastructure.web.dto.CurrentUserResponse;
import io.github.mongsil3344.qnow.user.infrastructure.web.dto.SignUpRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "계정", description = "회원가입, 로그인, 로그아웃, 내 계정 API")
@AllArgsConstructor
@RestController
public class UserController {

    private final SignUpService signUpService;

    @Operation(summary = "회원가입 API")
    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        signUpService.signUp(
                signUpRequest.email(),
                signUpRequest.nickname(),
                signUpRequest.password()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @Operation(summary = "내 계정 조회 API")
    @GetMapping("/users/me")
    public ResponseEntity<CurrentUserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(new CurrentUserResponse(
                principal.id(),
                principal.email(),
                principal.nickname()
        ));
    }
}
