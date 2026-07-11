package io.github.mongsil3344.qnow.question.infrastructure.web.dto;

import io.github.mongsil3344.qnow.question.application.dto.QuestionUpvoteResult;
import java.util.UUID;

public record QuestionUpvoteResponse(
    UUID questionId,
    boolean upvotedByMe,
    int upvoteCount
) {

    public static QuestionUpvoteResponse from(QuestionUpvoteResult result) {
        return new QuestionUpvoteResponse(
            result.questionId(),
            result.upvotedByMe(),
            result.upvoteCount()
        );
    }
}
