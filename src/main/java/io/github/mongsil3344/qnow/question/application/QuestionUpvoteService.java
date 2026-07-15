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
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.session.api.SessionStatusApi;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;
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
        return upvote(questionId, new SessionActor.Member(userId));
    }

    @Transactional
    public QuestionUpvoteResult upvote(UUID questionId, SessionActor actor) {
        Question question = findActiveQuestionForUpdate(questionId);
        UUID participantId = requireActiveParticipant(question, actor);

        validateNotQuestioner(question, actor, participantId);

        if (findQuestionUpvote(questionId, actor, participantId).isEmpty()) {
            questionUpvoteRepository.save(createQuestionUpvote(question, actor, participantId));
            question.incrementUpvoteCount();
        }

        return toResult(question, true);
    }

    @Transactional
    public QuestionUpvoteResult cancelUpvote(UUID questionId, UUID userId) {
        return cancelUpvote(questionId, new SessionActor.Member(userId));
    }

    @Transactional
    public QuestionUpvoteResult cancelUpvote(UUID questionId, SessionActor actor) {
        Question question = findActiveQuestionForUpdate(questionId);
        UUID participantId = requireActiveParticipant(question, actor);

        findQuestionUpvote(questionId, actor, participantId)
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

    private UUID requireActiveParticipant(
        Question question,
        SessionActor actor
    ) {
        UploadedPresentationInfo presentation = presentationQueryApi
            .findUploadedPresentationById(question.getPresentationId())
            .orElseThrow(QuestionNotFoundException::new);

        if (sessionQueryApi.findOrganizationIdBySessionId(presentation.sessionId()).isEmpty()) {
            throw new QuestionNotFoundException();
        }

        sessionStatusApi.requireNotEnded(presentation.sessionId());

        return sessionQueryApi.findActiveParticipantId(
                presentation.sessionId(),
                actor
            )
            .orElseThrow(SessionParticipantRequiredException::new);
    }

    private void validateNotQuestioner(Question question, SessionActor actor, UUID participantId) {
        boolean selfUpvote = switch (actor) {
            case SessionActor.Member member -> {
                UUID questionerUserId = sessionQueryApi
                    .findUserIdsByParticipantIds(Set.of(question.getQuestionerId()))
                    .get(question.getQuestionerId());
                yield member.userId().equals(questionerUserId);
            }
            case SessionActor.Guest ignored -> participantId.equals(question.getQuestionerId());
        };

        if (selfUpvote) {
            throw new SelfUpvoteNotAllowedException();
        }
    }

    private Optional<QuestionUpvote> findQuestionUpvote(
        UUID questionId,
        SessionActor actor,
        UUID participantId
    ) {
        return switch (actor) {
            case SessionActor.Member member ->
                questionUpvoteRepository.findByQuestionIdAndVoterUserId(questionId, member.userId());
            case SessionActor.Guest ignored ->
                questionUpvoteRepository.findByQuestionIdAndVoterGuestParticipantId(questionId, participantId);
        };
    }

    private QuestionUpvote createQuestionUpvote(Question question, SessionActor actor, UUID participantId) {
        return switch (actor) {
            case SessionActor.Member member -> QuestionUpvote.member(question, member.userId());
            case SessionActor.Guest ignored -> QuestionUpvote.guest(question, participantId);
        };
    }

    private QuestionUpvoteResult toResult(Question question, boolean upvotedByMe) {
        return new QuestionUpvoteResult(
            question.getId(),
            upvotedByMe,
            question.getUpvoteCount()
        );
    }
}
