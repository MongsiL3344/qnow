package io.github.mongsil3344.qnow.session.application.exception;

public class SessionParticipateCodeNotFoundException extends RuntimeException {

    public SessionParticipateCodeNotFoundException() {
        super("유효한 세션 참가 코드를 찾을 수 없습니다");
    }
}
