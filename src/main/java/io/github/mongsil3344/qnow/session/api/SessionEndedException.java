package io.github.mongsil3344.qnow.session.api;

public class SessionEndedException extends RuntimeException {

    public SessionEndedException() {
        super("이미 종료된 세션에서는 이 작업을 수행할 수 없습니다");
    }
}
