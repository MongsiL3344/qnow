package io.github.mongsil3344.qnow.presentation.application.dto;

import java.time.Instant;
import java.util.UUID;

public record PdfUrlResult(
        UUID presentationId,
        String pdfUrl,
        String contentType,
        Instant expiresAt
) {}
