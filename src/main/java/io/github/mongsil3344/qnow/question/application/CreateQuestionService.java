package io.github.mongsil3344.qnow.question.application;

import io.github.mongsil3344.qnow.presentation.api.PresentationQueryApi;
import io.github.mongsil3344.qnow.presentation.api.UploadedPresentationInfo;
import io.github.mongsil3344.qnow.question.application.dto.CreateQuestionCommand;
import io.github.mongsil3344.qnow.question.application.exception.InvalidQuestionReferenceException;
import io.github.mongsil3344.qnow.question.application.exception.QuestionPresentationNotFoundException;
import io.github.mongsil3344.qnow.question.application.exception.SessionParticipantRequiredException;
import io.github.mongsil3344.qnow.question.domain.Question;
import io.github.mongsil3344.qnow.question.domain.QuestionSelection;
import io.github.mongsil3344.qnow.question.infrastructure.repo.QuestionRepository;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
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
    private final QuestionRepository questionRepository;

    @Transactional
    public void createQuestion(UUID presentationId, UUID userId, CreateQuestionCommand command) {
        // 프레젠테이션id, 세션id, 페이지수 반환
        UploadedPresentationInfo presentation = presentationQueryApi.findUploadedPresentationById(presentationId)
                .orElseThrow(QuestionPresentationNotFoundException::new);

        // 위에서 구한 세션id에 해당 유저가 참여자로 있는지 검사
        UUID participantId = sessionQueryApi.findActiveParticipantId(presentation.sessionId(), userId)
                .orElseThrow(SessionParticipantRequiredException::new);

        // 페이지 수 유효성 검사
        validatePageReference(command.pageStart(), command.pageEnd(), presentation.pageCount());

        // 화면 선택영역 검사 + 정규화
        QuestionSelection selection = normalizeSelection(command);

        Question question = Question.builder()
                .presentationId(presentation.presentationId())
                .questionerId(participantId)
                .content(command.content().strip())
                .anonymous(command.anonymous())
                .pageStart(command.pageStart())
                .pageEnd(command.pageEnd())
                .selection(selection)
                .build();

        questionRepository.save(question);
    }


    /* -----------------------------------------------------------------*/
    /* -----------------------------Private-----------------------------*/
    /* -----------------------------------------------------------------*/

    // 페이지 수 검사
    private void validatePageReference(int pageStart, int pageEnd, int pageCount) {
        if (pageStart < 1 || pageEnd < pageStart || pageEnd > pageCount) {
            throw new InvalidQuestionReferenceException();
        }
    }

    // Selection값 검사 + 소수점자리 정규화
    private QuestionSelection normalizeSelection(CreateQuestionCommand command) {
        CreateQuestionCommand.Selection selection = command.selection();
        if (selection == null) {
            return null;
        }

        if (command.pageStart() != command.pageEnd() || hasMissingRatio(selection)) {
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

    private boolean hasMissingRatio(CreateQuestionCommand.Selection selection) {
        return selection.leftRatio() == null
                || selection.topRatio() == null
                || selection.widthRatio() == null
                || selection.heightRatio() == null;
    }

    private BigDecimal normalizeRatio(BigDecimal ratio) {
        return ratio.setScale(RATIO_SCALE, RoundingMode.HALF_UP);
    }
}
