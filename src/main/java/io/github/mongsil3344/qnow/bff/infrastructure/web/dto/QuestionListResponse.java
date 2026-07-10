package io.github.mongsil3344.qnow.bff.infrastructure.web.dto;

import io.github.mongsil3344.qnow.bff.application.dto.QuestionListResult;
import java.math.BigDecimal;
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
        SelectionResponse selection,
        int upvoteCount,
        Instant createdAt
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
                SelectionResponse.from(result.selection()),
                result.upvoteCount(),
                result.createdAt()
            );
        }
    }

    public record SelectionResponse(
        BigDecimal leftRatio,
        BigDecimal topRatio,
        BigDecimal widthRatio,
        BigDecimal heightRatio
    ) {

        private static SelectionResponse from(QuestionListResult.SelectionResult result) {
            if (result == null) {
                return null;
            }

            return new SelectionResponse(
                result.leftRatio(),
                result.topRatio(),
                result.widthRatio(),
                result.heightRatio()
            );
        }
    }
}
