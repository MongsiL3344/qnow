package io.github.mongsil3344.qnow.presentation.application.exception;

public class PresenterViewControlForbiddenException extends RuntimeException {

    public PresenterViewControlForbiddenException() {
        super("세션 개설자만 발표 화면을 변경할 수 있습니다");
    }
}
