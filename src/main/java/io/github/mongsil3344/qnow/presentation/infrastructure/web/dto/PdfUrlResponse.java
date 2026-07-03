package io.github.mongsil3344.qnow.presentation.infrastructure.web.dto;

import io.github.mongsil3344.qnow.presentation.application.dto.PdfUrlResult;
import java.time.Instant;
import java.util.UUID;

public record PdfUrlResponse(
        UUID presentationId,
        String pdfUrl,
        String contentType,
        Instant expiresAt
) {

    public static PdfUrlResponse from(PdfUrlResult result) {
        return new PdfUrlResponse(
                result.presentationId(),
                result.pdfUrl(),
                result.contentType(),
                result.expiresAt()
        );
    }
}
