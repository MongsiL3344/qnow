package io.github.mongsil3344.qnow.presentation.application.exception;

public class PresentationAccessForbiddenException extends RuntimeException {

    public PresentationAccessForbiddenException() {
        super("조직에 참여한 유저만 발표 자료를 조회할 수 있습니다");
    }
}
