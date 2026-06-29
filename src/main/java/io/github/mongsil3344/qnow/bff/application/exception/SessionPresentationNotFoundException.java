package io.github.mongsil3344.qnow.bff.application.exception;

public class SessionPresentationNotFoundException extends RuntimeException {

    public SessionPresentationNotFoundException() {
        super("존재하지 않는 세션입니다");
    }
}
