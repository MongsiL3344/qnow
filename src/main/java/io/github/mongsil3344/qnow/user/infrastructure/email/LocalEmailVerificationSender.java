package io.github.mongsil3344.qnow.user.infrastructure.email;

import io.github.mongsil3344.qnow.user.application.EmailVerificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "qnow.email-verification", name = "sender", havingValue = "local")
public class LocalEmailVerificationSender implements EmailVerificationSender {

    @Override
    public void send(String email, String code) {
        log.info("Local email verification code: email={}, code={}", email, code);
    }
}
