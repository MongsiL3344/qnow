package io.github.mongsil3344.qnow.bff.application.dto;

import io.github.mongsil3344.qnow.question.api.QuestionSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuestionListResult(
    List<QuestionResult> content,
    int page,
    int size,
    boolean hasNext
) {

    public record QuestionResult(
        UUID id,
        String content,
        String questionerName,
        boolean anonymous,
        boolean mine,
        int pageStart,
        int pageEnd,
        QuestionSummary.Selection selection,
        int upvoteCount,
        boolean upvotedByMe,
        Instant createdAt
    ) {
    }
}
