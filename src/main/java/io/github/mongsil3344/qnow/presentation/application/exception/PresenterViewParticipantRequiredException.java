package io.github.mongsil3344.qnow.presentation.application.exception;

public class PresenterViewParticipantRequiredException extends RuntimeException {

    public PresenterViewParticipantRequiredException() {
        super("활성 세션 참여자만 발표 화면을 조회할 수 있습니다");
    }
}
