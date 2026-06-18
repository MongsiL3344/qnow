package io.github.mongsil3344.qnow.organization.application.exception;

public class InvalidOrganizationSearchKeywordException extends RuntimeException {

    public InvalidOrganizationSearchKeywordException() {
        super("검색어를 입력해주세요");
    }
}
