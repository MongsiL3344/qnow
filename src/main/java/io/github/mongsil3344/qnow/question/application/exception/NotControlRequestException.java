package io.github.mongsil3344.qnow.question.application.exception;

public class NotControlRequestException extends RuntimeException {

    public NotControlRequestException() {
        super("발표 제어 요청이 아닌 질문은 승인할 수 없습니다");
    }
}
