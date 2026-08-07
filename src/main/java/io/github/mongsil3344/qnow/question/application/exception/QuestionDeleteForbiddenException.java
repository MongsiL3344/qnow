package io.github.mongsil3344.qnow.question.application.exception;

public class QuestionDeleteForbiddenException extends RuntimeException {

    public QuestionDeleteForbiddenException() {
        super("질문을 삭제할 권한이 없습니다");
    }
}
