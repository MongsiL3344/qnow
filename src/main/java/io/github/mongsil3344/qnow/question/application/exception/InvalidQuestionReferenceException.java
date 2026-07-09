package io.github.mongsil3344.qnow.question.application.exception;

public class InvalidQuestionReferenceException extends RuntimeException {

    public InvalidQuestionReferenceException() {
        super("질문의 페이지 또는 선택 영역이 올바르지 않습니다");
    }
}
