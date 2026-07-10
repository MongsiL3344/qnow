package io.github.mongsil3344.qnow.bff.application.exception;

public class QuestionListPresentationNotFoundException extends RuntimeException {

    public QuestionListPresentationNotFoundException() {
        super("질문을 조회할 발표 자료를 찾을 수 없습니다");
    }
}
