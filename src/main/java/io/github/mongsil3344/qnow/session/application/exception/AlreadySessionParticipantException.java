package io.github.mongsil3344.qnow.session.application.exception;

public class AlreadySessionParticipantException extends RuntimeException {

    public AlreadySessionParticipantException() {
        super("이미 세션에 참여한 유저입니다");
    }
}
