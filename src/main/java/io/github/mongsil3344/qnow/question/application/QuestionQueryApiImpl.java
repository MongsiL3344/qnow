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
    public QuestionSlice findQuestions(UUID presentationId, UUID viewerUserId, QuestionListQuery query) {
        Slice<Question> questions = questionRepository.findAllByPresentationIdAndDeletedAtIsNull(
            presentationId,
            PageRequest.of(query.page(), query.size(), toSort(query.sort()))
        );

        Set<UUID> upvotedQuestionIds = findUpvotedQuestionIds(questions, viewerUserId);

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

    private Set<UUID> findUpvotedQuestionIds(Slice<Question> questions, UUID viewerUserId) {
        if (questions.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(questionUpvoteRepository.findUpvotedQuestionIds(
            questions.getContent().stream()
                .map(Question::getId)
                .toList(),
            viewerUserId
        ));
    }

    private QuestionSummary toSummary(Question question, boolean upvotedByMe) {
        return new QuestionSummary(
            question.getId(),
            question.getQuestionerId(),
            question.getContent(),
            question.isAnonymous(),
            question.getPageStart(),
            question.getPageEnd(),
            toSelection(question.getSelection()),
            question.getUpvoteCount(),
            upvotedByMe,
            question.getCreatedAt()
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
