package io.github.mongsil3344.qnow.question.application;

import io.github.mongsil3344.qnow.presentation.api.PresentationQueryApi;
import io.github.mongsil3344.qnow.presentation.api.UploadedPresentationInfo;
import io.github.mongsil3344.qnow.question.application.dto.QuestionUpvoteResult;
import io.github.mongsil3344.qnow.question.application.exception.QuestionNotFoundException;
import io.github.mongsil3344.qnow.question.application.exception.SelfUpvoteNotAllowedException;
import io.github.mongsil3344.qnow.question.application.exception.SessionParticipantRequiredException;
import io.github.mongsil3344.qnow.question.domain.Question;
import io.github.mongsil3344.qnow.question.domain.QuestionUpvote;
import io.github.mongsil3344.qnow.question.infrastructure.repo.QuestionRepository;
import io.github.mongsil3344.qnow.question.infrastructure.repo.QuestionUpvoteRepository;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.session.api.SessionStatusApi;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class QuestionUpvoteService {

    private final PresentationQueryApi presentationQueryApi;
    private final SessionQueryApi sessionQueryApi;
    private final SessionStatusApi sessionStatusApi;
    private final QuestionRepository questionRepository;
    private final QuestionUpvoteRepository questionUpvoteRepository;

    @Transactional
    public QuestionUpvoteResult upvote(UUID questionId, UUID userId) {
        Question question = findActiveQuestionForUpdate(questionId);
        validateActiveParticipant(question, userId);

        validateNotQuestioner(question, userId);

        if (questionUpvoteRepository.findByQuestionIdAndVoterUserId(questionId, userId).isEmpty()) {
            questionUpvoteRepository.save(QuestionUpvote.builder()
                .question(question)
                .voterUserId(userId)
                .build());
            question.incrementUpvoteCount();
        }

        return toResult(question, true);
    }

    @Transactional
    public QuestionUpvoteResult cancelUpvote(UUID questionId, UUID userId) {
        Question question = findActiveQuestionForUpdate(questionId);
        validateActiveParticipant(question, userId);

        questionUpvoteRepository.findByQuestionIdAndVoterUserId(questionId, userId)
            .ifPresent(questionUpvote -> {
                question.decrementUpvoteCount();
                questionUpvoteRepository.delete(questionUpvote);
                questionUpvoteRepository.flush();
            });

        return toResult(question, false);
    }

    private Question findActiveQuestionForUpdate(UUID questionId) {
        return questionRepository.findActiveByIdForUpdate(questionId)
            .orElseThrow(QuestionNotFoundException::new);
    }

    private void validateActiveParticipant(Question question, UUID userId) {
        UploadedPresentationInfo presentation = presentationQueryApi
            .findUploadedPresentationById(question.getPresentationId())
            .orElseThrow(QuestionNotFoundException::new);

        if (sessionQueryApi.findOrganizationIdBySessionId(presentation.sessionId()).isEmpty()) {
            throw new QuestionNotFoundException();
        }

        sessionStatusApi.requireNotEnded(presentation.sessionId());

        if (!sessionQueryApi.isActiveParticipant(presentation.sessionId(), userId)) {
            throw new SessionParticipantRequiredException();
        }
    }

    private void validateNotQuestioner(Question question, UUID userId) {
        UUID questionerUserId = sessionQueryApi
            .findUserIdsByParticipantIds(Set.of(question.getQuestionerId()))
            .get(question.getQuestionerId());

        if (questionerUserId == null) {
            throw new QuestionNotFoundException();
        }

        if (userId.equals(questionerUserId)) {
            throw new SelfUpvoteNotAllowedException();
        }
    }

    private QuestionUpvoteResult toResult(Question question, boolean upvotedByMe) {
        return new QuestionUpvoteResult(
            question.getId(),
            upvotedByMe,
            question.getUpvoteCount()
        );
    }
}
