package io.github.mongsil3344.qnow.question.application.exception;

public class SelfUpvoteNotAllowedException extends RuntimeException {

    public SelfUpvoteNotAllowedException() {
        super("본인 질문에는 공감할 수 없습니다");
    }
}
