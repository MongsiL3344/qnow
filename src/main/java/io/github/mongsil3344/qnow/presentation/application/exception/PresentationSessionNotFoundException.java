package io.github.mongsil3344.qnow.presentation.application.exception;

public class PresentationSessionNotFoundException extends RuntimeException {

    public PresentationSessionNotFoundException() {
        super("존재하지 않는 세션입니다");
    }
}
