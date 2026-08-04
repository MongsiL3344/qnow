package io.github.mongsil3344.qnow.question.application;

import io.github.mongsil3344.qnow.presentation.api.PresentationQueryApi;
import io.github.mongsil3344.qnow.presentation.api.UploadedPresentationInfo;
import io.github.mongsil3344.qnow.question.application.exception.QuestionDeleteForbiddenException;
import io.github.mongsil3344.qnow.question.application.exception.QuestionNotFoundException;
import io.github.mongsil3344.qnow.question.application.exception.SessionParticipantRequiredException;
import io.github.mongsil3344.qnow.question.domain.Question;
import io.github.mongsil3344.qnow.question.infrastructure.repo.QuestionRepository;
import io.github.mongsil3344.qnow.session.api.SessionAccessApi;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.session.api.SessionStatusApi;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class DeleteQuestionService {

    private final PresentationQueryApi presentationQueryApi;
    private final SessionQueryApi sessionQueryApi;
    private final SessionStatusApi sessionStatusApi;
    private final SessionAccessApi sessionAccessApi;
    private final QuestionRepository questionRepository;

    @Transactional
    public void deleteQuestion(UUID questionId, SessionActor actor) {
        Question question = questionRepository.findActiveByIdForUpdate(questionId)
            .orElseThrow(QuestionNotFoundException::new);

        UploadedPresentationInfo presentation = presentationQueryApi
            .findUploadedPresentationById(question.getPresentationId())
            .orElseThrow(QuestionNotFoundException::new);

        UUID sessionId = presentation.sessionId();
        if (sessionQueryApi.findOrganizationIdBySessionId(sessionId).isEmpty()) {
            throw new QuestionNotFoundException();
        }

        sessionStatusApi.requireNotEnded(sessionId);

        UUID participantId = sessionQueryApi.findActiveParticipantId(sessionId, actor)
            .orElseThrow(SessionParticipantRequiredException::new);

        boolean canDelete = switch (actor) {
            case SessionActor.Member member -> {
                UUID questionerUserId = sessionQueryApi
                    .findUserIdsByParticipantIds(Set.of(question.getQuestionerId()))
                    .get(question.getQuestionerId());
                boolean isQuestioner = member.userId().equals(questionerUserId);
                yield isQuestioner || sessionAccessApi.isSessionCreator(sessionId, member.userId());
            }
            case SessionActor.Guest ignored -> participantId.equals(question.getQuestionerId());
        };

        if (!canDelete) {
            throw new QuestionDeleteForbiddenException();
        }

        question.delete();
    }
}
