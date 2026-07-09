package io.github.mongsil3344.qnow.question.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class QuestionSelection {

    @Column(name = "selection_left_ratio", precision = 6, scale = 5)
    private BigDecimal leftRatio;

    @Column(name = "selection_top_ratio", precision = 6, scale = 5)
    private BigDecimal topRatio;

    @Column(name = "selection_width_ratio", precision = 6, scale = 5)
    private BigDecimal widthRatio;

    @Column(name = "selection_height_ratio", precision = 6, scale = 5)
    private BigDecimal heightRatio;

    public QuestionSelection(
            BigDecimal leftRatio,
            BigDecimal topRatio,
            BigDecimal widthRatio,
            BigDecimal heightRatio
    ) {
        this.leftRatio = Objects.requireNonNull(leftRatio);
        this.topRatio = Objects.requireNonNull(topRatio);
        this.widthRatio = Objects.requireNonNull(widthRatio);
        this.heightRatio = Objects.requireNonNull(heightRatio);
    }
}
