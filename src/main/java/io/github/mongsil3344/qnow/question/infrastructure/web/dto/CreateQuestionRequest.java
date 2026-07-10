package io.github.mongsil3344.qnow.question.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlType.DEFAULT;
import java.math.BigDecimal;

public record CreateQuestionRequest(
        @NotBlank(message = "질문 내용을 입력해주세요")
        @Size(max = 500, message = "질문 내용은 500자 이하여야 합니다")
        String content,

        @JsonSetter(nulls = Nulls.AS_EMPTY)
        boolean anonymous,

        @NotNull(message = "질문 시작 페이지가 필요합니다")
        Integer pageStart,

        @NotNull(message = "질문 끝 페이지가 필요합니다")
        Integer pageEnd,

        @Valid
        SelectionRequest selection
) {

    public record SelectionRequest(
            BigDecimal leftRatio,
            BigDecimal topRatio,
            BigDecimal widthRatio,
            BigDecimal heightRatio
    ) {}
}
