package io.github.mongsil3344.qnow.organization.application.exception;

public class AlreadyOrganizationMemberException extends RuntimeException {

    public AlreadyOrganizationMemberException() {
        super("이미 조직에 참여한 유저입니다");
    }
}
