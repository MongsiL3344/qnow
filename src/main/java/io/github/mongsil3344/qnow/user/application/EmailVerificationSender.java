package io.github.mongsil3344.qnow.user.application;

public interface EmailVerificationSender {

    void send(String email, String code);
}
