package io.github.mongsil3344.qnow.presentation.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UploadCompleteRequest(
        @NotBlank
        String objectKey
) {}
