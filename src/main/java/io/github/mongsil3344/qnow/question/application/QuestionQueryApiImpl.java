package io.github.mongsil3344.qnow.question.application;

import io.github.mongsil3344.qnow.question.api.QuestionListQuery;
import io.github.mongsil3344.qnow.question.api.QuestionQueryApi;
import io.github.mongsil3344.qnow.question.api.QuestionSlice;
import io.github.mongsil3344.qnow.question.api.QuestionSort;
import io.github.mongsil3344.qnow.question.api.QuestionSummary;
import io.github.mongsil3344.qnow.question.domain.Question;
import io.github.mongsil3344.qnow.question.domain.QuestionSelection;
import io.github.mongsil3344.qnow.question.infrastructure.repo.QuestionRepository;
import io.github.mongsil3344.qnow.question.infrastructure.repo.QuestionUpvoteRepository;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Component
public class QuestionQueryApiImpl implements QuestionQueryApi {

    private final QuestionRepository questionRepository;
    private final QuestionUpvoteRepository questionUpvoteRepository;

    @Override
    @Transactional(readOnly = true)
    public QuestionSlice findQuestions(
        UUID presentationId,
        SessionActor viewer,
        QuestionListQuery query
    ) {
        Slice<Question> questions = questionRepository.findAllByPresentationIdAndDeletedAtIsNull(
            presentationId,
            PageRequest.of(query.page(), query.size(), toSort(query.sort()))
        );

        Set<UUID> upvotedQuestionIds = findUpvotedQuestionIds(
            questions,
            viewer
        );

        return new QuestionSlice(
            questions.getContent().stream()
                .map(question -> toSummary(question, upvotedQuestionIds.contains(question.getId())))
                .toList(),
            questions.getNumber(),
            questions.getSize(),
            questions.hasNext()
        );
    }

    private Sort toSort(QuestionSort sort) {
        return switch (sort) {
            case LATEST -> Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
            );
            case OLDEST -> Sort.by(
                Sort.Order.asc("createdAt"),
                Sort.Order.asc("id")
            );
            case MOST_UPVOTED -> Sort.by(
                Sort.Order.desc("upvoteCount"),
                Sort.Order.asc("createdAt"),
                Sort.Order.asc("id")
            );
            case PAGE_START_ASC -> Sort.by(
                Sort.Order.asc("pageStart"),
                Sort.Order.asc("pageEnd"),
                Sort.Order.asc("createdAt"),
                Sort.Order.asc("id")
            );
        };
    }

    private Set<UUID> findUpvotedQuestionIds(
        Slice<Question> questions,
        SessionActor viewer
    ) {
        if (questions.isEmpty()) {
            return Set.of();
        }

        var questionIds = questions.getContent().stream()
            .map(Question::getId)
            .toList();

        return switch (viewer) {
            case SessionActor.Member member -> Set.copyOf(
                questionUpvoteRepository.findUpvotedQuestionIds(questionIds, member.userId())
            );
            case SessionActor.Guest guest -> Set.copyOf(
                questionUpvoteRepository.findGuestUpvotedQuestionIds(
                    questionIds,
                    guest.participantId()
                )
            );
        };
    }

    private QuestionSummary toSummary(Question question, boolean upvotedByMe) {
        return new QuestionSummary(
            question.getId(),
            question.getQuestionerId(),
            question.getKind().name(),
            question.getContent(),
            question.isAnonymous(),
            question.getPageStart(),
            question.getPageEnd(),
            toSelection(question.getSelection()),
            question.getUpvoteCount(),
            upvotedByMe,
            question.getCreatedAt(),
            question.getApprovedAt()
        );
    }

    private QuestionSummary.Selection toSelection(QuestionSelection selection) {
        if (selection == null) {
            return null;
        }

        return new QuestionSummary.Selection(
            selection.getLeftRatio(),
            selection.getTopRatio(),
            selection.getWidthRatio(),
            selection.getHeightRatio()
        );
    }
}
