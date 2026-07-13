package io.github.mongsil3344.qnow.global.security;

import io.github.mongsil3344.qnow.global.security.dto.CsrfTokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
public class CsrfController {

    private final CsrfTokenRepository csrfTokenRepository;

    @GetMapping("/csrf")
    public ResponseEntity<CsrfTokenResponse> getCsrfToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        CsrfToken csrfToken = csrfTokenRepository.loadToken(request);

        if (csrfToken == null) {
            csrfToken = csrfTokenRepository.generateToken(request);
            csrfTokenRepository.saveToken(csrfToken, request, response);
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(CsrfTokenResponse.from(csrfToken));
    }
}
