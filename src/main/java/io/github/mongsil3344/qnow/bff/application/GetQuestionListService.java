package io.github.mongsil3344.qnow.bff.application;

import io.github.mongsil3344.qnow.bff.application.dto.QuestionListResult;
import io.github.mongsil3344.qnow.bff.application.exception.InvalidQuestionListQueryException;
import io.github.mongsil3344.qnow.bff.application.exception.QuestionListPresentationNotFoundException;
import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.presentation.api.PresentationQueryApi;
import io.github.mongsil3344.qnow.presentation.api.UploadedPresentationInfo;
import io.github.mongsil3344.qnow.question.api.QuestionListQuery;
import io.github.mongsil3344.qnow.question.api.QuestionQueryApi;
import io.github.mongsil3344.qnow.question.api.QuestionSlice;
import io.github.mongsil3344.qnow.question.api.QuestionSort;
import io.github.mongsil3344.qnow.question.api.QuestionSummary;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.user.api.UserQueryApi;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class GetQuestionListService {

    private static final String ANONYMOUS_NAME = "익명";
    private static final String UNKNOWN_USER_NAME = "알 수 없는 사용자";
    private static final int MAX_PAGE_SIZE = 100;

    private final OrganizationQueryApi organizationQueryApi;
    private final PresentationQueryApi presentationQueryApi;
    private final QuestionQueryApi questionQueryApi;
    private final SessionQueryApi sessionQueryApi;
    private final UserQueryApi userQueryApi;

    @Transactional(readOnly = true)
    public QuestionListResult getQuestions(
        UUID presentationId,
        UUID currentUserId,
        QuestionSort sort,
        int page,
        int size
    ) {
        validatePagination(page, size);

        UploadedPresentationInfo presentation = presentationQueryApi.findUploadedPresentationById(presentationId)
            .orElseThrow(QuestionListPresentationNotFoundException::new);
        UUID organizationId = sessionQueryApi.findOrganizationIdBySessionId(presentation.sessionId())
            .orElseThrow(QuestionListPresentationNotFoundException::new);

        organizationQueryApi.getOrganizationInfo(organizationId, currentUserId);

        QuestionSlice questions = questionQueryApi.findQuestions(
            presentationId,
            new QuestionListQuery(sort, page, size)
        );

        if (questions.content().isEmpty()) {
            return new QuestionListResult(
                List.of(),
                questions.page(),
                questions.size(),
                questions.hasNext()
            );
        }

        // 질문 -> 참여자ID 추출
        Set<UUID> participantIds = questions.content().stream()
            .map(QuestionSummary::questionerParticipantId)
            .collect(Collectors.toSet());

        // 참여자ID -> 사용자ID 추출
        Map<UUID, UUID> userIdsByParticipantId =
            sessionQueryApi.findUserIdsByParticipantIds(participantIds);

        // 익명이 아닌 질문자의 아이디를 모아서 Set으로 만듦
        Set<UUID> visibleQuestionerUserIds = questions.content().stream()
            .filter(question -> !question.anonymous())
            .map(QuestionSummary::questionerParticipantId)
            .map(userIdsByParticipantId::get)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());

        // 익명이 아닌 사용자들의 아이디를 다 넘겨서 닉네임을 뽑음
        Map<UUID, String> nicknamesByUserId = userQueryApi.findNicknamesByIds(visibleQuestionerUserIds);


        return new QuestionListResult(
            questions.content().stream()
                .map(question -> toResult(
                    question,
                    currentUserId,
                    userIdsByParticipantId,
                    nicknamesByUserId
                ))
                .toList(),
            questions.page(),
            questions.size(),
            questions.hasNext()
        );
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidQuestionListQueryException();
        }
    }

    private QuestionListResult.QuestionResult toResult(
        QuestionSummary question,
        UUID currentUserId,
        Map<UUID, UUID> userIdsByParticipantId,
        Map<UUID, String> nicknamesByUserId
    ) {
        UUID questionerUserId = userIdsByParticipantId.get(question.questionerParticipantId());

        return new QuestionListResult.QuestionResult(
            question.id(),
            question.content(),
            resolveQuestionerName(question, questionerUserId, nicknamesByUserId),
            question.anonymous(),
            currentUserId.equals(questionerUserId),
            question.pageStart(),
            question.pageEnd(),
            toSelectionResult(question.selection()),
            question.upvoteCount(),
            question.createdAt()
        );
    }

    private String resolveQuestionerName(
        QuestionSummary question,
        UUID questionerUserId,
        Map<UUID, String> nicknamesByUserId
    ) {
        if (question.anonymous()) {
            return ANONYMOUS_NAME;
        }

        if (questionerUserId == null) {
            return UNKNOWN_USER_NAME;
        }

        return nicknamesByUserId.getOrDefault(questionerUserId, UNKNOWN_USER_NAME);
    }

    private QuestionListResult.SelectionResult toSelectionResult(QuestionSummary.Selection selection) {
        if (selection == null) {
            return null;
        }

        return new QuestionListResult.SelectionResult(
            selection.leftRatio(),
            selection.topRatio(),
            selection.widthRatio(),
            selection.heightRatio()
        );
    }

}
