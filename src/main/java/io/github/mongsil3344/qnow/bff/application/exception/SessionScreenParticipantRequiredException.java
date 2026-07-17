package io.github.mongsil3344.qnow.bff.application.exception;

public class SessionScreenParticipantRequiredException extends RuntimeException {

    public SessionScreenParticipantRequiredException() {
        super("활성 세션 참여자만 세션 화면을 조회할 수 있습니다");
    }
}
