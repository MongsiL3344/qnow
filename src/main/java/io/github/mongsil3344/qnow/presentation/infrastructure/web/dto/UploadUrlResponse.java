package io.github.mongsil3344.qnow.presentation.infrastructure.web.dto;

import io.github.mongsil3344.qnow.presentation.application.dto.UploadUrlResult;
import java.time.Instant;
import java.util.UUID;

public record UploadUrlResponse(
        UUID presentationId,
        String uploadUrl,
        String objectKey,
        Instant expiresAt
) {

    public static UploadUrlResponse from(UploadUrlResult result) {
        return new UploadUrlResponse(
                result.presentationId(),
                result.uploadUrl(),
                result.objectKey(),
                result.expiresAt()
        );
    }
}
