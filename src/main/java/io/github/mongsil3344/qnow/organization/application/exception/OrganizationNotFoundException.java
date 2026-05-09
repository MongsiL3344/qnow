package io.github.mongsil3344.qnow.organization.application.exception;

public class OrganizationNotFoundException extends RuntimeException {

    public OrganizationNotFoundException() {
        super("존재하지 않는 조직입니다");
    }
}
