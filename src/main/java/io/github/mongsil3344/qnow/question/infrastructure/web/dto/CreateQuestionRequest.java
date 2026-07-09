package io.github.mongsil3344.qnow.question.infrastructure.web.dto;

import io.github.mongsil3344.qnow.question.application.dto.CreateQuestionCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 요청은 CreateQuestionRequest로 받고
 * 서비스에 넘길 객체는 toCommand로 만들어서 CreateQuestionCommand로 넘김
 */
public record CreateQuestionRequest(
        @NotBlank(message = "질문 내용을 입력해주세요")
        @Size(max = 500, message = "질문 내용은 500자 이하여야 합니다")
        String content,

        @NotNull(message = "질문 시작 페이지가 필요합니다")
        Integer pageStart,

        @NotNull(message = "질문 끝 페이지가 필요합니다")
        Integer pageEnd,

        @Valid
        SelectionRequest selection
) {

    public CreateQuestionCommand toCommand() {
        return new CreateQuestionCommand(
                content,
                pageStart,
                pageEnd,
                selection == null ? null : selection.toCommand()
        );
    }

    public record SelectionRequest(
            BigDecimal leftRatio,
            BigDecimal topRatio,
            BigDecimal widthRatio,
            BigDecimal heightRatio
    ) {

        private CreateQuestionCommand.Selection toCommand() {
            return new CreateQuestionCommand.Selection(
                    leftRatio,
                    topRatio,
                    widthRatio,
                    heightRatio
            );
        }
    }
}
