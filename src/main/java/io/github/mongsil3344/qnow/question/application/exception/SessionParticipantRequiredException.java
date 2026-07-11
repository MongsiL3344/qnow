package io.github.mongsil3344.qnow.question.application.exception;

public class SessionParticipantRequiredException extends RuntimeException {

    public SessionParticipantRequiredException() {
        super("현재 세션에 참여 중인 사용자만 질문을 등록하거나 공감할 수 있습니다");
    }
}
