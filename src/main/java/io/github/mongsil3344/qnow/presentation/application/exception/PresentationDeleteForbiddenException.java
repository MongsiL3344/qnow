package io.github.mongsil3344.qnow.presentation.application.exception;

public class PresentationDeleteForbiddenException extends RuntimeException {

    public PresentationDeleteForbiddenException() {
        super("발표 자료를 삭제할 권한이 없습니다");
    }
}
