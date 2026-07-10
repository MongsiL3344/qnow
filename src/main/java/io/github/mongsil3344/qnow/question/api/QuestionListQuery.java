package io.github.mongsil3344.qnow.question.api;

public record QuestionListQuery(
    QuestionSort sort,
    int page,
    int size
) {
}
