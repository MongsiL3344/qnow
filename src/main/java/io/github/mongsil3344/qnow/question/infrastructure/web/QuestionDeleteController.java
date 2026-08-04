package io.github.mongsil3344.qnow.question.infrastructure.web;

import io.github.mongsil3344.qnow.question.application.DeleteQuestionService;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "질문", description = "발표 자료 질문 API")
@AllArgsConstructor
@RestController
public class QuestionDeleteController {

    private final DeleteQuestionService deleteQuestionService;

    @Operation(summary = "질문 삭제 API")
    @ApiResponse(responseCode = "204", description = "질문 삭제 성공")
    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
        @Parameter(hidden = true) SessionActor actor,
        @PathVariable UUID questionId
    ) {
        deleteQuestionService.deleteQuestion(questionId, actor);
        return ResponseEntity.noContent().build();
    }
}
