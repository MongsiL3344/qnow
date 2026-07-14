package io.github.mongsil3344.qnow.presentation.application.exception;

public class InvalidPresenterViewPageException extends RuntimeException {

    public InvalidPresenterViewPageException() {
        super("발표 페이지 번호가 올바르지 않습니다");
    }
}
