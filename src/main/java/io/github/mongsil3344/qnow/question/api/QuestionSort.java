package io.github.mongsil3344.qnow.question.api;

import java.util.Arrays;
import java.util.Optional;

public enum QuestionSort {
    LATEST("latest"),
    OLDEST("oldest"),
    MOST_UPVOTED("most_upvoted"),
    PAGE_START_ASC("page_start_asc");

    private final String queryValue;

    QuestionSort(String queryValue) {
        this.queryValue = queryValue;
    }

    public static Optional<QuestionSort> fromQueryValue(String value) {
        return Arrays.stream(values())
            .filter(sort -> sort.queryValue.equals(value))
            .findFirst();
    }
}
