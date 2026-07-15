package io.github.mongsil3344.qnow.question.application;

import io.github.mongsil3344.qnow.presentation.api.PresentationQueryApi;
import io.github.mongsil3344.qnow.presentation.api.UploadedPresentationInfo;
import io.github.mongsil3344.qnow.question.application.exception.InvalidQuestionReferenceException;
import io.github.mongsil3344.qnow.question.application.exception.QuestionPresentationNotFoundException;
import io.github.mongsil3344.qnow.question.application.exception.SessionParticipantRequiredException;
import io.github.mongsil3344.qnow.question.domain.Question;
import io.github.mongsil3344.qnow.question.domain.QuestionSelection;
import io.github.mongsil3344.qnow.question.infrastructure.repo.QuestionRepository;
import io.github.mongsil3344.qnow.question.infrastructure.web.dto.CreateQuestionRequest.SelectionRequest;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.session.api.SessionStatusApi;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class CreateQuestionService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final int RATIO_SCALE = 5;

    private final PresentationQueryApi presentationQueryApi;
    private final SessionQueryApi sessionQueryApi;
    private final SessionStatusApi sessionStatusApi;
    private final QuestionRepository questionRepository;

    @Transactional
    public void createQuestion(
            UUID presentationId,
            UUID userId,
            String content,
            boolean anonymous,
            int pageStart,
            int pageEnd,
            SelectionRequest selectionRequest
    ) {
        createQuestion(
            presentationId,
            new SessionActor.Member(userId),
            content,
            anonymous,
            pageStart,
            pageEnd,
            selectionRequest
        );
    }

    @Transactional
    public void createQuestion(
            UUID presentationId,
            SessionActor actor,
            String content,
            boolean anonymous,
            int pageStart,
            int pageEnd,
            SelectionRequest selectionRequest
    ) {
        UploadedPresentationInfo presentation = presentationQueryApi.findUploadedPresentationById(presentationId)
                .orElseThrow(QuestionPresentationNotFoundException::new);

        sessionStatusApi.requireNotEnded(presentation.sessionId());

        UUID participantId = sessionQueryApi.findActiveParticipantId(
                presentation.sessionId(),
                actor
            )
                .orElseThrow(SessionParticipantRequiredException::new);

        validatePageReference(pageStart, pageEnd, presentation.pageCount());

        QuestionSelection selection = normalizeSelection(pageStart, pageEnd, selectionRequest);

        Question question = Question.builder()
                .presentationId(presentation.presentationId())
                .questionerId(participantId)
                .content(content.strip())
                .anonymous(anonymous)
                .pageStart(pageStart)
                .pageEnd(pageEnd)
                .selection(selection)
                .build();

        questionRepository.save(question);
    }

    private void validatePageReference(int pageStart, int pageEnd, int pageCount) {
        if (pageStart < 1 || pageEnd < pageStart || pageEnd > pageCount) {
            throw new InvalidQuestionReferenceException();
        }
    }

    private QuestionSelection normalizeSelection(
            int pageStart,
            int pageEnd,
            SelectionRequest selection
    ) {
        if (selection == null) {
            return null;
        }

        if (pageStart != pageEnd || hasMissingRatio(selection)) {
            throw new InvalidQuestionReferenceException();
        }

        BigDecimal left = selection.leftRatio();
        BigDecimal top = selection.topRatio();
        BigDecimal width = selection.widthRatio();
        BigDecimal height = selection.heightRatio();
        BigDecimal right = left.add(width);
        BigDecimal bottom = top.add(height);

        if (left.compareTo(ZERO) < 0
                || top.compareTo(ZERO) < 0
                || width.compareTo(ZERO) <= 0
                || height.compareTo(ZERO) <= 0
                || right.compareTo(ONE) > 0
                || bottom.compareTo(ONE) > 0) {
            throw new InvalidQuestionReferenceException();
        }

        BigDecimal normalizedLeft = normalizeRatio(left);
        BigDecimal normalizedTop = normalizeRatio(top);
        BigDecimal normalizedRight = normalizeRatio(right);
        BigDecimal normalizedBottom = normalizeRatio(bottom);
        BigDecimal normalizedWidth = normalizedRight.subtract(normalizedLeft);
        BigDecimal normalizedHeight = normalizedBottom.subtract(normalizedTop);

        if (normalizedWidth.compareTo(ZERO) <= 0 || normalizedHeight.compareTo(ZERO) <= 0) {
            throw new InvalidQuestionReferenceException();
        }

        return new QuestionSelection(
                normalizedLeft,
                normalizedTop,
                normalizedWidth,
                normalizedHeight
        );
    }

    private boolean hasMissingRatio(SelectionRequest selection) {
        return selection.leftRatio() == null
                || selection.topRatio() == null
                || selection.widthRatio() == null
                || selection.heightRatio() == null;
    }

    private BigDecimal normalizeRatio(BigDecimal ratio) {
        return ratio.setScale(RATIO_SCALE, RoundingMode.HALF_UP);
    }
}
