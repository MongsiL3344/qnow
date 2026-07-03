package io.github.mongsil3344.qnow.presentation.application.exception;

public class PresentationNotFoundException extends RuntimeException {

    public PresentationNotFoundException() {
        super("존재하지 않는 발표 자료입니다");
    }
}
