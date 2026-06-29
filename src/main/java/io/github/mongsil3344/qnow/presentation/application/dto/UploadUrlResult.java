package io.github.mongsil3344.qnow.presentation.application.dto;

import java.time.Instant;
import java.util.UUID;

public record UploadUrlResult(
        UUID presentationId,
        String uploadUrl,
        String objectKey,
        Instant expiresAt
) {}
