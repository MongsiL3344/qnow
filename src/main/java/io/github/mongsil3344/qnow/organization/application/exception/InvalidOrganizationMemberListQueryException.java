package io.github.mongsil3344.qnow.organization.application.exception;

public class InvalidOrganizationMemberListQueryException extends RuntimeException {

    public InvalidOrganizationMemberListQueryException() {
        super("조직 멤버 목록 조회 조건이 올바르지 않습니다");
    }
}
