package io.github.mongsil3344.qnow.global.error;

import io.github.mongsil3344.qnow.bff.application.exception.SessionPresentationNotFoundException;
import io.github.mongsil3344.qnow.bff.application.exception.SessionPresentationParticipantRequiredException;
import io.github.mongsil3344.qnow.bff.application.exception.SessionScreenNotFoundException;
import io.github.mongsil3344.qnow.bff.application.exception.SessionScreenOrganizationMemberRequiredException;
import io.github.mongsil3344.qnow.bff.application.exception.SessionScreenParticipantRequiredException;
import io.github.mongsil3344.qnow.bff.application.exception.InvalidQuestionListQueryException;
import io.github.mongsil3344.qnow.bff.application.exception.QuestionListPresentationNotFoundException;
import io.github.mongsil3344.qnow.bff.application.exception.QuestionListParticipantRequiredException;
import io.github.mongsil3344.qnow.organization.application.exception.DuplicateNameException;
import io.github.mongsil3344.qnow.organization.application.exception.AlreadyOrganizationMemberException;
import io.github.mongsil3344.qnow.organization.application.exception.InvalidOrganizationPasswordException;
import io.github.mongsil3344.qnow.organization.application.exception.InvalidOrganizationMemberListQueryException;
import io.github.mongsil3344.qnow.organization.application.exception.InvalidOrganizationSearchKeywordException;
import io.github.mongsil3344.qnow.organization.application.exception.OrganizationMemberRequiredException;
import io.github.mongsil3344.qnow.organization.application.exception.OrganizationNotFoundException;
import io.github.mongsil3344.qnow.organization.application.exception.UserNotFoundException;
import io.github.mongsil3344.qnow.presentation.application.exception.InvalidUploadObjectKeyException;
import io.github.mongsil3344.qnow.presentation.application.exception.InvalidPresenterViewPageException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationAccessForbiddenException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationDeleteForbiddenException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationNotFoundException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationObjectNotFoundException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationSessionNotFoundException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationUploadForbiddenException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresenterViewControlForbiddenException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresenterViewParticipantRequiredException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresenterViewUnavailableException;
import io.github.mongsil3344.qnow.question.application.exception.GuestUpvoteNotAllowedException;
import io.github.mongsil3344.qnow.question.application.exception.InvalidQuestionReferenceException;
import io.github.mongsil3344.qnow.question.application.exception.QuestionDeleteForbiddenException;
import io.github.mongsil3344.qnow.question.application.exception.QuestionNotFoundException;
import io.github.mongsil3344.qnow.question.application.exception.QuestionPresentationNotFoundException;
import io.github.mongsil3344.qnow.question.application.exception.SelfUpvoteNotAllowedException;
import io.github.mongsil3344.qnow.question.application.exception.SessionParticipantRequiredException;
import io.github.mongsil3344.qnow.session.api.SessionEndedException;
import io.github.mongsil3344.qnow.session.application.exception.AlreadySessionParticipantException;
import io.github.mongsil3344.qnow.session.application.exception.NotOrganizationMemberException;
import io.github.mongsil3344.qnow.session.application.exception.OrganizationAdminRequiredException;
import io.github.mongsil3344.qnow.session.application.exception.SessionNotFoundException;
import io.github.mongsil3344.qnow.session.application.exception.SessionParticipateCodeNotFoundException;
import io.github.mongsil3344.qnow.user.application.exception.DuplicateEmailException;
import io.github.mongsil3344.qnow.user.application.exception.DuplicateNicknameException;
import io.github.mongsil3344.qnow.user.application.exception.EmailVerificationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    // 유저 - 닉네임 중복 예외
    @ExceptionHandler(DuplicateNicknameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateNickname(DuplicateNicknameException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_NICKNAME", e.getMessage()));
    }

    @ExceptionHandler(EmailVerificationException.class)
    public ResponseEntity<ErrorResponse> handleEmailVerification(EmailVerificationException e) {
        HttpStatus status = switch (e.error()) {
            case REQUEST_TOO_FREQUENT, REQUEST_LIMIT_EXCEEDED, ATTEMPTS_EXCEEDED ->
                HttpStatus.TOO_MANY_REQUESTS;
            case CODE_EXPIRED -> HttpStatus.GONE;
            case CODE_MISMATCH -> HttpStatus.BAD_REQUEST;
            case REQUIRED -> HttpStatus.FORBIDDEN;
            case DELIVERY_FAILED, UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };

        return ResponseEntity
            .status(status)
            .body(new ErrorResponse("EMAIL_VERIFICATION_" + e.error().name(), e.getMessage()));
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

    @ExceptionHandler(InvalidOrganizationSearchKeywordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrganizationSearchKeyword(
        InvalidOrganizationSearchKeywordException e
    ) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INVALID_ORGANIZATION_SEARCH_KEYWORD", e.getMessage()));
    }

    @ExceptionHandler(InvalidOrganizationMemberListQueryException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrganizationMemberListQuery(
        InvalidOrganizationMemberListQueryException e
    ) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INVALID_ORGANIZATION_MEMBER_LIST_QUERY", e.getMessage()));
    }

    @ExceptionHandler(OrganizationMemberRequiredException.class)
    public ResponseEntity<ErrorResponse> handleOrganizationMemberRequired(OrganizationMemberRequiredException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ORGANIZATION_MEMBER_REQUIRED", e.getMessage()));
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

    @ExceptionHandler(SessionEndedException.class)
    public ResponseEntity<ErrorResponse> handleSessionEnded(SessionEndedException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("SESSION_ENDED", e.getMessage()));
    }

    @ExceptionHandler(AlreadySessionParticipantException.class)
    public ResponseEntity<ErrorResponse> handleAlreadySessionParticipant(AlreadySessionParticipantException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("ALREADY_SESSION_PARTICIPANT", e.getMessage()));
    }

    @ExceptionHandler(SessionParticipateCodeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSessionParticipateCodeNotFound(
        SessionParticipateCodeNotFoundException e
    ) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("SESSION_PARTICIPATE_CODE_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(InvalidUploadObjectKeyException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUploadObjectKey(InvalidUploadObjectKeyException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_UPLOAD_OBJECT_KEY", e.getMessage()));
    }

    @ExceptionHandler(PresentationObjectNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePresentationObjectNotFound(PresentationObjectNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("PRESENTATION_OBJECT_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(PresentationUploadForbiddenException.class)
    public ResponseEntity<ErrorResponse> handlePresentationUploadForbidden(PresentationUploadForbiddenException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("PRESENTATION_UPLOAD_FORBIDDEN", e.getMessage()));
    }

    @ExceptionHandler(PresentationAccessForbiddenException.class)
    public ResponseEntity<ErrorResponse> handlePresentationAccessForbidden(PresentationAccessForbiddenException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("PRESENTATION_ACCESS_FORBIDDEN", e.getMessage()));
    }

    @ExceptionHandler(PresentationDeleteForbiddenException.class)
    public ResponseEntity<ErrorResponse> handlePresentationDeleteForbidden(PresentationDeleteForbiddenException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("PRESENTATION_DELETE_FORBIDDEN", e.getMessage()));
    }

    @ExceptionHandler(PresentationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePresentationNotFound(PresentationNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("PRESENTATION_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(PresentationSessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePresentationSessionNotFound(PresentationSessionNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("SESSION_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(InvalidPresenterViewPageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPresenterViewPage(InvalidPresenterViewPageException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INVALID_PRESENTER_VIEW_PAGE", e.getMessage()));
    }

    @ExceptionHandler(PresenterViewParticipantRequiredException.class)
    public ResponseEntity<ErrorResponse> handlePresenterViewParticipantRequired(
        PresenterViewParticipantRequiredException e
    ) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("SESSION_PARTICIPANT_REQUIRED", e.getMessage()));
    }

    @ExceptionHandler(PresenterViewControlForbiddenException.class)
    public ResponseEntity<ErrorResponse> handlePresenterViewControlForbidden(
        PresenterViewControlForbiddenException e
    ) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("PRESENTER_VIEW_CONTROL_FORBIDDEN", e.getMessage()));
    }

    @ExceptionHandler(PresenterViewUnavailableException.class)
    public ResponseEntity<ErrorResponse> handlePresenterViewUnavailable(PresenterViewUnavailableException e) {
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse("PRESENTER_VIEW_UNAVAILABLE", e.getMessage()));
    }

    @ExceptionHandler(SessionPresentationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSessionPresentationNotFound(SessionPresentationNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("SESSION_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(SessionPresentationParticipantRequiredException.class)
    public ResponseEntity<ErrorResponse> handleSessionPresentationParticipantRequired(
        SessionPresentationParticipantRequiredException e
    ) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("SESSION_PARTICIPANT_REQUIRED", e.getMessage()));
    }

    @ExceptionHandler(SessionScreenNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSessionScreenNotFound(SessionScreenNotFoundException e) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("SESSION_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(SessionScreenOrganizationMemberRequiredException.class)
    public ResponseEntity<ErrorResponse> handleSessionScreenOrganizationMemberRequired(
        SessionScreenOrganizationMemberRequiredException e
    ) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("ORGANIZATION_MEMBER_REQUIRED", e.getMessage()));
    }

    @ExceptionHandler(SessionScreenParticipantRequiredException.class)
    public ResponseEntity<ErrorResponse> handleSessionScreenParticipantRequired(
        SessionScreenParticipantRequiredException e
    ) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("SESSION_PARTICIPANT_REQUIRED", e.getMessage()));
    }

    @ExceptionHandler(QuestionListPresentationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleQuestionListPresentationNotFound(
        QuestionListPresentationNotFoundException e
    ) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("PRESENTATION_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(QuestionListParticipantRequiredException.class)
    public ResponseEntity<ErrorResponse> handleQuestionListParticipantRequired(
        QuestionListParticipantRequiredException e
    ) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("SESSION_PARTICIPANT_REQUIRED", e.getMessage()));
    }

    @ExceptionHandler(InvalidQuestionListQueryException.class)
    public ResponseEntity<ErrorResponse> handleInvalidQuestionListQuery(InvalidQuestionListQueryException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INVALID_INPUT", e.getMessage()));
    }

    @ExceptionHandler(QuestionPresentationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleQuestionPresentationNotFound(QuestionPresentationNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("PRESENTATION_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(SessionParticipantRequiredException.class)
    public ResponseEntity<ErrorResponse> handleSessionParticipantRequired(SessionParticipantRequiredException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("SESSION_PARTICIPANT_REQUIRED", e.getMessage()));
    }

    @ExceptionHandler(QuestionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleQuestionNotFound(QuestionNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("QUESTION_NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(QuestionDeleteForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleQuestionDeleteForbidden(QuestionDeleteForbiddenException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("QUESTION_DELETE_FORBIDDEN", e.getMessage()));
    }

    @ExceptionHandler(SelfUpvoteNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleSelfUpvoteNotAllowed(SelfUpvoteNotAllowedException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("SELF_UPVOTE_NOT_ALLOWED", e.getMessage()));
    }

    @ExceptionHandler(GuestUpvoteNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleGuestUpvoteNotAllowed(GuestUpvoteNotAllowedException e) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("GUEST_UPVOTE_NOT_ALLOWED", e.getMessage()));
    }

    @ExceptionHandler(InvalidQuestionReferenceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidQuestionReference(InvalidQuestionReferenceException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_QUESTION_REFERENCE", e.getMessage()));
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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INVALID_INPUT", "입력값이 올바르지 않습니다"));
    }

    // 그 외 전역 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다"));
    }
}
