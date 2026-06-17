package io.github.mongsil3344.qnow.organization.application.exception;

public class OrganizationMemberRequiredException extends RuntimeException {

    public OrganizationMemberRequiredException() {
        super("조직에 참여한 유저만 조회할 수 있습니다");
    }
}
