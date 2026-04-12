package io.github.mongsil3344.qnow.session.application.exception;

public class NotOrganizationMemberException extends RuntimeException {

    public NotOrganizationMemberException() {
        super("유저 또는 조직 정보가 올바르지 않습니다");
    }
}
