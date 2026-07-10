package io.github.mongsil3344.qnow.bff.application.exception;

public class InvalidQuestionListQueryException extends RuntimeException {

    public InvalidQuestionListQueryException() {
        super("질문 목록 조회 조건이 올바르지 않습니다");
    }
}
