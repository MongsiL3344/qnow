package io.github.mongsil3344.qnow.organization.application.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("존재하지 않는 유저는 조직을 개설할 수 없습니다");
    }
}
