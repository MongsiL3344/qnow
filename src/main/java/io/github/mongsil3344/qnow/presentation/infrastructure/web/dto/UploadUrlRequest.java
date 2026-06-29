package io.github.mongsil3344.qnow.presentation.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UploadUrlRequest(
        @NotBlank
        @Size(max = 255)
        String title
) {}
