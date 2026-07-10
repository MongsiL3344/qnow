package io.github.mongsil3344.qnow.question.api;

import java.util.List;

public record QuestionSlice(
    List<QuestionSummary> content,
    int page,
    int size,
    boolean hasNext
) {
}
