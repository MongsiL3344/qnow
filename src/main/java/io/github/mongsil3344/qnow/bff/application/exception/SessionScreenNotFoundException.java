package io.github.mongsil3344.qnow.bff.application.exception;

public class SessionScreenNotFoundException extends RuntimeException {

    public SessionScreenNotFoundException() {
        super("존재하지 않는 세션입니다");
    }
}
