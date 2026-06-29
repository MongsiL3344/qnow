package io.github.mongsil3344.qnow.presentation.application.exception;

public class PresentationObjectNotFoundException extends RuntimeException {

    public PresentationObjectNotFoundException(Throwable cause) {
        super("S3에 업로드된 발표 자료를 찾을 수 없습니다", cause);
    }
}
