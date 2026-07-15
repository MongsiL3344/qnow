package io.github.mongsil3344.qnow.bff.application.exception;

public class QuestionListParticipantRequiredException extends RuntimeException {

    public QuestionListParticipantRequiredException() {
        super("활성 세션 참여자만 질문을 조회할 수 있습니다");
    }
}
