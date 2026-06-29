package io.github.mongsil3344.qnow.presentation.application.exception;

public class PresentationUploadForbiddenException extends RuntimeException {

    public PresentationUploadForbiddenException() {
        super("조직에 참여한 유저만 발표자료를 업로드할 수 있습니다");
    }
}
