package io.github.mongsil3344.qnow.presentation.application.exception;

public class PresenterControlTargetInvalidException extends RuntimeException {

    public PresenterControlTargetInvalidException() {
        super("제어권을 부여할 수 없는 참여자입니다");
    }
}
