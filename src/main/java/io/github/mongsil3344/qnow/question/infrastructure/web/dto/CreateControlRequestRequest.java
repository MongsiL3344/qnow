package io.github.mongsil3344.qnow.question.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

public record CreateControlRequestRequest(
        @NotNull(message = "발표 제어 요청 페이지가 필요합니다")
        Integer pageNumber
) {
}
