package io.github.mongsil3344.qnow.question.application.exception;

public class QuestionPresentationNotFoundException extends RuntimeException {

    public QuestionPresentationNotFoundException() {
        super("질문을 등록할 발표 자료를 찾을 수 없습니다");
    }
}
