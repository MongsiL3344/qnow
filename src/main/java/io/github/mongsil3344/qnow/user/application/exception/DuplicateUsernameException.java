package io.github.mongsil3344.qnow.user.application.exception;

public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException() {
        super("이미 사용 중인 아이디입니다");
    }
}
