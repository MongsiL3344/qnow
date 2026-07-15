package io.github.mongsil3344.qnow.presentation.infrastructure.web;

import io.github.mongsil3344.qnow.presentation.application.CompleteUploadService;
import io.github.mongsil3344.qnow.presentation.application.DeletePresentationService;
import io.github.mongsil3344.qnow.presentation.application.PdfUrlService;
import io.github.mongsil3344.qnow.presentation.application.UploadUrlService;
import io.github.mongsil3344.qnow.presentation.application.dto.PdfUrlResult;
import io.github.mongsil3344.qnow.presentation.application.dto.UploadUrlResult;
import io.github.mongsil3344.qnow.presentation.infrastructure.web.dto.PdfUrlResponse;
import io.github.mongsil3344.qnow.presentation.infrastructure.web.dto.UploadCompleteRequest;
import io.github.mongsil3344.qnow.presentation.infrastructure.web.dto.UploadUrlRequest;
import io.github.mongsil3344.qnow.presentation.infrastructure.web.dto.UploadUrlResponse;
import io.github.mongsil3344.qnow.user.api.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "프레젠테이션", description = "발표 자료 API")
@AllArgsConstructor
@RequestMapping("/organizations/{organizationId}/sessions/{sessionId}/presentations")
@RestController
public class PresentationController {

    private final UploadUrlService uploadUrlService;
    private final CompleteUploadService completeUploadService;
    private final PdfUrlService pdfUrlService;
    private final DeletePresentationService deletePresentationService;

    @Operation(summary = "발표 자료 업로드 URL 발급 API")
    @PostMapping("/upload-url")
    public ResponseEntity<UploadUrlResponse> createUploadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID organizationId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody UploadUrlRequest request
    ) {
        UploadUrlResult result = uploadUrlService.createUploadUrl(
                organizationId,
                sessionId,
                principal.id(),
                request.title(),
                request.pageCount()
        );

        return ResponseEntity.ok(UploadUrlResponse.from(result));
    }

    @Operation(summary = "발표 자료 업로드 완료 API")
    @PostMapping("/upload-complete")
    public ResponseEntity<Void> completeUpload(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID organizationId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody UploadCompleteRequest request
    ) {
        completeUploadService.completeUpload(
                organizationId,
                sessionId,
                principal.id(),
                request.objectKey()
        );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .build();
    }

    @Operation(summary = "발표 자료 PDF 조회 URL 발급 API")
    @GetMapping("/{presentationId}/pdf")
    public ResponseEntity<PdfUrlResponse> createPdfUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID organizationId,
            @PathVariable UUID sessionId,
            @PathVariable UUID presentationId
    ) {
        PdfUrlResult result = pdfUrlService.createPdfUrl(
                organizationId,
                sessionId,
                presentationId,
                principal.id()
        );

        return ResponseEntity.ok(PdfUrlResponse.from(result));
    }

    @Operation(summary = "발표 자료 삭제 API")
    @DeleteMapping("/{presentationId}")
    public ResponseEntity<Void> deletePresentation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID organizationId,
            @PathVariable UUID sessionId,
            @PathVariable UUID presentationId
    ) {
        deletePresentationService.deletePresentation(
                organizationId,
                sessionId,
                presentationId,
                principal.id()
        );

        return ResponseEntity.noContent().build();
    }
}
