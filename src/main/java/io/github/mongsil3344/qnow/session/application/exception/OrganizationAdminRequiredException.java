package io.github.mongsil3344.qnow.session.application.exception;

public class OrganizationAdminRequiredException extends RuntimeException {

    public OrganizationAdminRequiredException() {
        super("조직 관리자만 세션을 개설할 수 있습니다");
    }
}
