package io.github.mongsil3344.qnow.presentation.application.exception;

public class InvalidUploadObjectKeyException extends RuntimeException {

    public InvalidUploadObjectKeyException() {
        super("발표 자료 업로드 경로가 올바르지 않습니다");
    }
}
