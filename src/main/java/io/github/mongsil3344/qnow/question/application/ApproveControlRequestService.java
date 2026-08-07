package io.github.mongsil3344.qnow.question.application;

import io.github.mongsil3344.qnow.presentation.api.PresentationQueryApi;
import io.github.mongsil3344.qnow.presentation.api.UploadedPresentationInfo;
import io.github.mongsil3344.qnow.question.application.exception.ControlRequestApprovalForbiddenException;
import io.github.mongsil3344.qnow.question.application.exception.NotControlRequestException;
import io.github.mongsil3344.qnow.question.application.exception.QuestionNotFoundException;
import io.github.mongsil3344.qnow.question.domain.Question;
import io.github.mongsil3344.qnow.question.domain.QuestionKind;
import io.github.mongsil3344.qnow.question.infrastructure.repo.QuestionRepository;
import io.github.mongsil3344.qnow.session.api.SessionAccessApi;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class ApproveControlRequestService {

    private final PresentationQueryApi presentationQueryApi;
    private final SessionAccessApi sessionAccessApi;
    private final QuestionRepository questionRepository;

    @Transactional
    public void approveControlRequest(UUID questionId, UUID userId) {
        Question question = questionRepository.findActiveByIdForUpdate(questionId)
                .orElseThrow(QuestionNotFoundException::new);

        if (question.getKind() != QuestionKind.CONTROL_REQUEST) {
            throw new NotControlRequestException();
        }

        UUID sessionId = presentationQueryApi
                .findUploadedPresentationById(question.getPresentationId())
                .map(UploadedPresentationInfo::sessionId)
                .orElseThrow(QuestionNotFoundException::new);

        if (!sessionAccessApi.isSessionCreator(sessionId, userId)) {
            throw new ControlRequestApprovalForbiddenException();
        }

        question.approve(Instant.now());
    }
}
