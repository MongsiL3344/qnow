package io.github.mongsil3344.qnow.bff.infrastructure.web;

import io.github.mongsil3344.qnow.bff.application.GetQuestionListService;
import io.github.mongsil3344.qnow.bff.application.exception.InvalidQuestionListQueryException;
import io.github.mongsil3344.qnow.bff.infrastructure.web.dto.QuestionListResponse;
import io.github.mongsil3344.qnow.question.api.QuestionSort;
import io.github.mongsil3344.qnow.user.api.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "질문", description = "발표 자료 질문 API")
@AllArgsConstructor
@RequestMapping("/presentations/{presentationId}/questions")
@RestController
public class QuestionListController {

    private final GetQuestionListService getQuestionListService;

    @Operation(summary = "질문 목록 조회 API")
    @GetMapping
    public ResponseEntity<QuestionListResponse> getQuestions(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID presentationId,
        @RequestParam(defaultValue = "latest") String sort,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        QuestionSort questionSort = QuestionSort.fromQueryValue(sort)
            .orElseThrow(InvalidQuestionListQueryException::new);

        return ResponseEntity.ok(
            QuestionListResponse.from(
                getQuestionListService.getQuestions(
                    presentationId,
                    principal.id(),
                    questionSort,
                    page,
                    size
                )
            )
        );
    }
}
