package io.github.mongsil3344.qnow.question.application.dto;

import java.util.UUID;

public record QuestionUpvoteResult(
    UUID questionId,
    boolean upvotedByMe,
    int upvoteCount
) {}
