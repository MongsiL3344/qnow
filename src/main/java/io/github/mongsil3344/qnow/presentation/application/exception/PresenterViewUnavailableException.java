package io.github.mongsil3344.qnow.presentation.application.exception;

public class PresenterViewUnavailableException extends RuntimeException {

    public PresenterViewUnavailableException(Throwable cause) {
        super("발표 화면 동기화 서비스를 사용할 수 없습니다", cause);
    }

    public PresenterViewUnavailableException(String message) {
        super(message);
    }
}
