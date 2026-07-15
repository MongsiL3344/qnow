package io.github.mongsil3344.qnow.question.infrastructure.web;

import io.github.mongsil3344.qnow.question.application.CreateQuestionService;
import io.github.mongsil3344.qnow.question.infrastructure.web.dto.CreateQuestionRequest;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "질문", description = "발표 자료 질문 API")
@AllArgsConstructor
@RequestMapping("/presentations/{presentationId}/questions")
@RestController
public class QuestionController {

    private final CreateQuestionService createQuestionService;

    @Operation(summary = "질문 등록 API")
    @PostMapping
    public ResponseEntity<Void> createQuestion(
            @Parameter(hidden = true) SessionActor actor,
            @PathVariable UUID presentationId,
            @Valid @RequestBody CreateQuestionRequest request
    ) {
        createQuestionService.createQuestion(
                presentationId,
                actor,
                request.content(),
                request.anonymous(),
                request.pageStart(),
                request.pageEnd(),
                request.selection()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }
}
