package io.github.mongsil3344.qnow.question.application.exception;

public class GuestUpvoteNotAllowedException extends RuntimeException {

    public GuestUpvoteNotAllowedException() {
        super("비회원 공감을 허용하지 않는 세션입니다");
    }
}
