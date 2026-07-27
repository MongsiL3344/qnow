package io.github.mongsil3344.qnow.user.application.exception;

public class EmailVerificationException extends RuntimeException {

    private final Error error;

    public EmailVerificationException(Error error) {
        super(error.message);
        this.error = error;
    }

    public EmailVerificationException(Error error, Throwable cause) {
        super(error.message, cause);
        this.error = error;
    }

    public Error error() {
        return error;
    }

    public enum Error {
        REQUEST_TOO_FREQUENT("인증번호는 잠시 후 다시 요청해 주세요"),
        REQUEST_LIMIT_EXCEEDED("인증번호 요청 횟수를 초과했습니다"),
        CODE_EXPIRED("인증번호가 만료되었거나 존재하지 않습니다"),
        CODE_MISMATCH("인증번호가 올바르지 않습니다"),
        ATTEMPTS_EXCEEDED("인증번호 확인 시도 횟수를 초과했습니다"),
        REQUIRED("이메일 인증이 필요합니다"),
        DELIVERY_FAILED("인증 이메일을 전송할 수 없습니다"),
        UNAVAILABLE("이메일 인증 서비스를 사용할 수 없습니다");

        private final String message;

        Error(String message) {
            this.message = message;
        }
    }
}
