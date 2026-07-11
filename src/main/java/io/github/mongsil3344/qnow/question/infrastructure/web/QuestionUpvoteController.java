package io.github.mongsil3344.qnow.question.infrastructure.web;

import io.github.mongsil3344.qnow.question.application.QuestionUpvoteService;
import io.github.mongsil3344.qnow.question.infrastructure.web.dto.QuestionUpvoteResponse;
import io.github.mongsil3344.qnow.user.api.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "질문", description = "발표 자료 질문 API")
@AllArgsConstructor
@RequestMapping("/questions/{questionId}/upvote")
@RestController
public class QuestionUpvoteController {

    private final QuestionUpvoteService questionUpvoteService;

    @Operation(summary = "질문 공감 등록 API")
    @PutMapping
    public ResponseEntity<QuestionUpvoteResponse> upvote(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID questionId
    ) {
        return ResponseEntity.ok(
            QuestionUpvoteResponse.from(questionUpvoteService.upvote(questionId, principal.id()))
        );
    }

    @Operation(summary = "질문 공감 취소 API")
    @DeleteMapping
    public ResponseEntity<QuestionUpvoteResponse> cancelUpvote(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID questionId
    ) {
        return ResponseEntity.ok(
            QuestionUpvoteResponse.from(questionUpvoteService.cancelUpvote(questionId, principal.id()))
        );
    }
}
