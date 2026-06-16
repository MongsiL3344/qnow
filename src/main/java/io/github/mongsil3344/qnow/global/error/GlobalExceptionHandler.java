package io.github.mongsil3344.qnow.global.error;

import io.github.mongsil3344.qnow.organization.application.exception.DuplicateNameException;
import io.github.mongsil3344.qnow.organization.application.exception.AlreadyOrganizationMemberException;
import io.github.mongsil3344.qnow.organization.application.exception.InvalidOrganizationPasswordException;
import io.github.mongsil3344.qnow.organization.application.exception.OrganizationNotFoundException;
import io.github.mongsil3344.qnow.organization.application.exception.UserNotFoundException;
import io.github.mongsil3344.qnow.session.application.exception.AlreadySessionParticipantException;
import io.github.mongsil3344.qnow.session.application.exception.NotOrganizationMemberException;
import io.github.mongsil3344.qnow.session.application.exception.OrganizationAdminRequiredException;
import io.github.mongsil3344.qnow.session.application.exception.SessionNotFoundException;
import io.github.mongsil3344.qnow.user.application.exception.DuplicateEmailException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 유저 - 이메일 중복 예외
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_EMAIL", e.getMessage()));
    }

    // 조직 - 조직명 중복 예외
    @ExceptionHandler(DuplicateNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateOrganizationName(DuplicateNameException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_NAME", e.getMessage()));
    }

    // 유저그룹 - 존재하지 않는 유저의 조직개설 예외
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("USER_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(OrganizationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrganizationNotFound(OrganizationNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("ORGANIZATION_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(AlreadyOrganizationMemberException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyOrganizationMember(AlreadyOrganizationMemberException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("ALREADY_ORGANIZATION_MEMBER", e.getMessage()));
    }

    @ExceptionHandler(InvalidOrganizationPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrganizationPassword(InvalidOrganizationPasswordException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("INVALID_ORGANIZATION_PASSWORD", e.getMessage()));
    }

    // 세션 - 조직에 소속되어있지 않은 유저의 세션 개설 요청
    @ExceptionHandler(NotOrganizationMemberException.class)
    public ResponseEntity<ErrorResponse> handleNotOrganizationMember(NotOrganizationMemberException e) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("NOT_ORGANIZATION_MEMBER", e.getMessage()));
    }

    @ExceptionHandler(OrganizationAdminRequiredException.class)
    public ResponseEntity<ErrorResponse> handleOrganizationAdminRequired(OrganizationAdminRequiredException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ORGANIZATION_ADMIN_REQUIRED", e.getMessage()));
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSessionNotFound(SessionNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("SESSION_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(AlreadySessionParticipantException.class)
    public ResponseEntity<ErrorResponse> handleAlreadySessionParticipant(AlreadySessionParticipantException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("ALREADY_SESSION_PARTICIPANT", e.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("AUTHENTICATION_FAILED", "이메일 또는 비밀번호가 올바르지 않습니다"));
    }


    // DTO - 필드 검증 예외
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "입력값이 올바르지 않습니다";

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_INPUT", message));
    }

    // 그 외 전역 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다"));
    }
}
