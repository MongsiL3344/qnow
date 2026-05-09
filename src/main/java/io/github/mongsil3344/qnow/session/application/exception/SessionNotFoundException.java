package io.github.mongsil3344.qnow.session.application.exception;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException() {
        super("존재하지 않는 세션입니다");
    }
}
