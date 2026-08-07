package io.github.mongsil3344.qnow.bff.infrastructure.web.dto;

import io.github.mongsil3344.qnow.bff.application.dto.QuestionListResult;
import io.github.mongsil3344.qnow.question.api.QuestionSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuestionListResponse(
    List<QuestionResponse> content,
    int page,
    int size,
    boolean hasNext
) {

    public static QuestionListResponse from(QuestionListResult result) {
        return new QuestionListResponse(
            result.content().stream()
                .map(QuestionResponse::from)
                .toList(),
            result.page(),
            result.size(),
            result.hasNext()
        );
    }

    public record QuestionResponse(
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
        Instant createdAt,
        String kind,
        boolean approved,
        UUID questionerParticipantId
    ) {

        private static QuestionResponse from(QuestionListResult.QuestionResult result) {
            return new QuestionResponse(
                result.id(),
                result.content(),
                result.questionerName(),
                result.anonymous(),
                result.mine(),
                result.pageStart(),
                result.pageEnd(),
                result.selection(),
                result.upvoteCount(),
                result.upvotedByMe(),
                result.createdAt(),
                result.kind(),
                result.approved(),
                result.questionerParticipantId()
            );
        }
    }
}
