package io.github.mongsil3344.qnow.organization.application.exception;

public class InvalidOrganizationPasswordException extends RuntimeException {

    public InvalidOrganizationPasswordException() {
        super("조직 비밀번호가 올바르지 않습니다");
    }
}
