package io.github.mongsil3344.qnow.bff.application.exception;

public class SessionPresentationParticipantRequiredException extends RuntimeException {

    public SessionPresentationParticipantRequiredException() {
        super("활성 세션 참여자만 발표 자료를 조회할 수 있습니다");
    }
}
