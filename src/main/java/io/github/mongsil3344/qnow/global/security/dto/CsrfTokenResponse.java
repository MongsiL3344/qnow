package io.github.mongsil3344.qnow.global.security.dto;

import org.springframework.security.web.csrf.CsrfToken;

public record CsrfTokenResponse(
        String token
) {

    public static CsrfTokenResponse from(CsrfToken csrfToken) {
        return new CsrfTokenResponse(
                csrfToken.getToken()
        );
    }
}
