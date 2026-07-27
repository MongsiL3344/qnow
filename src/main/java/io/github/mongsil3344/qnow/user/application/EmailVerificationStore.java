package io.github.mongsil3344.qnow.user.application;

public interface EmailVerificationStore {

    RequestResult createRequest(String email, String code);

    void cancelRequest(String email, String code);

    VerificationResult verify(String email, String code);

    boolean isVerified(String email);

    void clearVerification(String email);

    enum RequestResult {
        CREATED,
        ALREADY_VERIFIED,
        TOO_FREQUENT,
        REQUEST_LIMIT_EXCEEDED
    }

    enum VerificationResult {
        VERIFIED,
        ALREADY_VERIFIED,
        CODE_EXPIRED,
        CODE_MISMATCH,
        ATTEMPTS_EXCEEDED
    }
}
