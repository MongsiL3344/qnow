package io.github.mongsil3344.qnow.bff.application.exception;

public class SessionScreenOrganizationMemberRequiredException extends RuntimeException {

    public SessionScreenOrganizationMemberRequiredException() {
        super("조직에 가입한 회원만 세션 화면을 조회할 수 있습니다");
    }
}
