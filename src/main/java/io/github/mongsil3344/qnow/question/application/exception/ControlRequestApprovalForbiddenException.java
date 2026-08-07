package io.github.mongsil3344.qnow.question.application.exception;

public class ControlRequestApprovalForbiddenException extends RuntimeException {

    public ControlRequestApprovalForbiddenException() {
        super("세션 개설자만 발표 제어 요청을 승인할 수 있습니다");
    }
}
