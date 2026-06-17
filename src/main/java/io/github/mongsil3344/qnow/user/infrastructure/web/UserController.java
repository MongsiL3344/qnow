package io.github.mongsil3344.qnow.user.infrastructure.web;

import io.github.mongsil3344.qnow.user.application.SignUpService;
import io.github.mongsil3344.qnow.user.api.UserPrincipal;
import io.github.mongsil3344.qnow.user.infrastructure.web.dto.CurrentUserResponse;
import io.github.mongsil3344.qnow.user.infrastructure.web.dto.SignUpRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
public class UserController {

    private final SignUpService signUpService;

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

    @GetMapping("/users/me")
    public ResponseEntity<CurrentUserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(new CurrentUserResponse(
                principal.id(),
                principal.email(),
                principal.nickname()
        ));
    }
}
