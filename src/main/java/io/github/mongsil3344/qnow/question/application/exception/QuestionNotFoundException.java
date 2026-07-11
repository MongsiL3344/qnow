package io.github.mongsil3344.qnow.question.application.exception;

public class QuestionNotFoundException extends RuntimeException {

    public QuestionNotFoundException() {
        super("존재하지 않거나 유효하지 않은 질문입니다");
    }
}
