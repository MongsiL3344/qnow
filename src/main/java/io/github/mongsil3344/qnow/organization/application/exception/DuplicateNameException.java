package io.github.mongsil3344.qnow.organization.application.exception;

public class DuplicateNameException extends RuntimeException {

    public DuplicateNameException() {
        super("이미 사용중인 조직명입니다");
    }
}
