package io.github.mongsil3344.qnow.question.api;

import java.util.UUID;

public interface QuestionQueryApi {

    QuestionSlice findQuestions(UUID presentationId, QuestionListQuery query);
}
