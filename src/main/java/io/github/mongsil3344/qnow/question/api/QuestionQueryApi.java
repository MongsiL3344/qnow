package io.github.mongsil3344.qnow.question.api;

import io.github.mongsil3344.qnow.session.api.SessionActor;
import java.util.UUID;

public interface QuestionQueryApi {

    default QuestionSlice findQuestions(UUID presentationId, UUID viewerUserId, QuestionListQuery query) {
        return findQuestions(
            presentationId,
            new SessionActor.Member(viewerUserId),
            query
        );
    }

    QuestionSlice findQuestions(
        UUID presentationId,
        SessionActor viewer,
        QuestionListQuery query
    );
}
