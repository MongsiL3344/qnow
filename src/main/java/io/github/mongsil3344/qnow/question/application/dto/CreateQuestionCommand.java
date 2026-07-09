package io.github.mongsil3344.qnow.question.application.dto;

import java.math.BigDecimal;

public record CreateQuestionCommand(
        String content,
        int pageStart,
        int pageEnd,
        Selection selection
) {

    public record Selection(
            BigDecimal leftRatio,
            BigDecimal topRatio,
            BigDecimal widthRatio,
            BigDecimal heightRatio
    ) {}
}
