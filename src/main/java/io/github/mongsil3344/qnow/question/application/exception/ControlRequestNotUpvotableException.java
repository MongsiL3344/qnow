package io.github.mongsil3344.qnow.question.application.exception;

public class ControlRequestNotUpvotableException extends RuntimeException {

    public ControlRequestNotUpvotableException() {
        super("발표 제어 요청에는 공감할 수 없습니다");
    }
}
