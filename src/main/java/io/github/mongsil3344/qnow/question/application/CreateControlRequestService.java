package io.github.mongsil3344.qnow.question.application;

import io.github.mongsil3344.qnow.presentation.api.PresentationQueryApi;
import io.github.mongsil3344.qnow.presentation.api.UploadedPresentationInfo;
import io.github.mongsil3344.qnow.question.application.exception.InvalidQuestionReferenceException;
import io.github.mongsil3344.qnow.question.application.exception.QuestionPresentationNotFoundException;
import io.github.mongsil3344.qnow.question.application.exception.SessionParticipantRequiredException;
import io.github.mongsil3344.qnow.question.domain.Question;
import io.github.mongsil3344.qnow.question.infrastructure.repo.QuestionRepository;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.session.api.SessionStatusApi;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class CreateControlRequestService {

    private final PresentationQueryApi presentationQueryApi;
    private final SessionQueryApi sessionQueryApi;
    private final SessionStatusApi sessionStatusApi;
    private final QuestionRepository questionRepository;

    @Transactional
    public void createControlRequest(
            UUID presentationId,
            SessionActor actor,
            int pageNumber
    ) {
        UploadedPresentationInfo presentation = presentationQueryApi
                .findUploadedPresentationById(presentationId)
                .orElseThrow(QuestionPresentationNotFoundException::new);

        sessionStatusApi.requireNotEnded(presentation.sessionId());

        UUID participantId = sessionQueryApi.findActiveParticipantId(
                        presentation.sessionId(),
                        actor
                )
                .orElseThrow(SessionParticipantRequiredException::new);

        validatePageNumber(pageNumber, presentation.pageCount());

        questionRepository.save(
                Question.controlRequest(
                        presentation.presentationId(),
                        participantId,
                        pageNumber
                )
        );
    }

    private void validatePageNumber(int pageNumber, int pageCount) {
        if (pageNumber < 1 || pageNumber > pageCount) {
            throw new InvalidQuestionReferenceException();
        }
    }
}
