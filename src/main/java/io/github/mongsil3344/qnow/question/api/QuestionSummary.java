package io.github.mongsil3344.qnow.question.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record QuestionSummary(
    UUID id,
    UUID questionerParticipantId,
    String content,
    boolean anonymous,
    int pageStart,
    int pageEnd,
    Selection selection,
    int upvoteCount,
    boolean upvotedByMe,
    Instant createdAt
) {

    public record Selection(
        BigDecimal leftRatio,
        BigDecimal topRatio,
        BigDecimal widthRatio,
        BigDecimal heightRatio
    ) {
    }
}
