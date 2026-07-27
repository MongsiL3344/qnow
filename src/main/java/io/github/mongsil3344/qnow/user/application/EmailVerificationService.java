package io.github.mongsil3344.qnow.user.application;

import static io.github.mongsil3344.qnow.user.application.EmailVerificationStore.RequestResult.ALREADY_VERIFIED;
import static io.github.mongsil3344.qnow.user.application.EmailVerificationStore.RequestResult.CREATED;
import static io.github.mongsil3344.qnow.user.application.exception.EmailVerificationException.Error.ATTEMPTS_EXCEEDED;
import static io.github.mongsil3344.qnow.user.application.exception.EmailVerificationException.Error.CODE_EXPIRED;
import static io.github.mongsil3344.qnow.user.application.exception.EmailVerificationException.Error.CODE_MISMATCH;
import static io.github.mongsil3344.qnow.user.application.exception.EmailVerificationException.Error.DELIVERY_FAILED;
import static io.github.mongsil3344.qnow.user.application.exception.EmailVerificationException.Error.REQUEST_LIMIT_EXCEEDED;
import static io.github.mongsil3344.qnow.user.application.exception.EmailVerificationException.Error.REQUEST_TOO_FREQUENT;
import static io.github.mongsil3344.qnow.user.application.exception.EmailVerificationException.Error.REQUIRED;

import io.github.mongsil3344.qnow.user.application.EmailVerificationStore.RequestResult;
import io.github.mongsil3344.qnow.user.application.EmailVerificationStore.VerificationResult;
import io.github.mongsil3344.qnow.user.application.exception.DuplicateEmailException;
import io.github.mongsil3344.qnow.user.application.exception.EmailVerificationException;
import io.github.mongsil3344.qnow.user.infrastructure.repo.UserRepository;
import java.security.SecureRandom;
import java.util.Locale;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EmailVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailVerificationStore verificationStore;
    private final EmailVerificationSender verificationSender;

    public void requestCode(String email) {
        String normalizedEmail = normalize(email);

        if (userRepository.existsByEmailAndDeletedAtIsNull(normalizedEmail)) {
            throw new DuplicateEmailException();
        }

        String code = String.format(Locale.ROOT, "%06d", SECURE_RANDOM.nextInt(1_000_000));
        RequestResult result = verificationStore.createRequest(normalizedEmail, code);

        if (result == ALREADY_VERIFIED) {
            return;
        }
        if (result == EmailVerificationStore.RequestResult.TOO_FREQUENT) {
            throw new EmailVerificationException(REQUEST_TOO_FREQUENT);
        }
        if (result == EmailVerificationStore.RequestResult.REQUEST_LIMIT_EXCEEDED) {
            throw new EmailVerificationException(REQUEST_LIMIT_EXCEEDED);
        }
        if (result != CREATED) {
            throw new IllegalStateException("Unknown email verification request result: " + result);
        }

        try {
            verificationSender.send(normalizedEmail, code);
        } catch (RuntimeException exception) {
            try {
                verificationStore.cancelRequest(normalizedEmail, code);
            } catch (RuntimeException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw new EmailVerificationException(DELIVERY_FAILED, exception);
        }
    }

    public void verifyCode(String email, String code) {
        VerificationResult result = verificationStore.verify(normalize(email), code);

        switch (result) {
            case VERIFIED, ALREADY_VERIFIED -> {
            }
            case CODE_EXPIRED -> throw new EmailVerificationException(CODE_EXPIRED);
            case CODE_MISMATCH -> throw new EmailVerificationException(CODE_MISMATCH);
            case ATTEMPTS_EXCEEDED -> throw new EmailVerificationException(ATTEMPTS_EXCEEDED);
        }
    }

    public void requireVerified(String email) {
        if (!verificationStore.isVerified(normalize(email))) {
            throw new EmailVerificationException(REQUIRED);
        }
    }

    public void clearVerification(String email) {
        verificationStore.clearVerification(normalize(email));
    }

    private String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
