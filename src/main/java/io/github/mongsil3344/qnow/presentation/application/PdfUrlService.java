package io.github.mongsil3344.qnow.presentation.application;

import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.presentation.application.dto.PdfUrlResult;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationAccessForbiddenException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationNotFoundException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationSessionNotFoundException;
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.domain.UploadStatus;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
public class PdfUrlService {

    private final S3Presigner s3Presigner;
    private final PresentationRepository presentationRepository;
    private final OrganizationQueryApi organizationQueryApi;
    private final SessionQueryApi sessionQueryApi;
    private final String bucket;
    private final Duration pdfUrlExpires;

    public PdfUrlService(
            S3Presigner s3Presigner,
            PresentationRepository presentationRepository,
            OrganizationQueryApi organizationQueryApi,
            SessionQueryApi sessionQueryApi,
            @Value("${qnow.storage.s3.bucket:}") String bucket,
            @Value("${qnow.storage.s3.pdf-url-expires-seconds:600}") long pdfUrlExpiresSeconds
    ) {
        this.s3Presigner = s3Presigner;
        this.presentationRepository = presentationRepository;
        this.organizationQueryApi = organizationQueryApi;
        this.sessionQueryApi = sessionQueryApi;
        this.bucket = bucket;
        this.pdfUrlExpires = Duration.ofSeconds(pdfUrlExpiresSeconds);
    }

    @Transactional(readOnly = true)
    public PdfUrlResult createPdfUrl(UUID organizationId, UUID sessionId, UUID presentationId, UUID userId) {
        return createPdfUrl(
            organizationId,
            sessionId,
            presentationId,
            new SessionActor.Member(userId)
        );
    }

    @Transactional(readOnly = true)
    public PdfUrlResult createPdfUrl(
            UUID organizationId,
            UUID sessionId,
            UUID presentationId,
            SessionActor actor
    ) {
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("S3 bucket is not configured");
        }

        validateAccessPermission(organizationId, sessionId, actor);

        Presentation presentation = presentationRepository
                .findByIdAndSessionIdAndUploadStatusAndDeletedAtIsNull(
                        presentationId,
                        sessionId,
                        UploadStatus.UPLOADED
                )
                .orElseThrow(PresentationNotFoundException::new);

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(pdfUrlExpires)
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(presentation.getS3Key())
                                .responseContentType(presentation.getContentType())
                                .build())
                        .build()
        );

        return new PdfUrlResult(
                presentation.getId(),
                presignedRequest.url().toString(),
                presentation.getContentType(),
                Instant.now().plus(pdfUrlExpires)
        );
    }

    private void validateAccessPermission(
        UUID organizationId,
        UUID sessionId,
        SessionActor actor
    ) {
        if (actor instanceof SessionActor.Member member) {
            boolean existsUserInOrganization = organizationQueryApi.existsUserInOrganization(
                member.userId(),
                organizationId
            );
            if (!existsUserInOrganization) {
                throw new PresentationAccessForbiddenException();
            }
        }

        boolean existsSessionInOrganization = sessionQueryApi.existsSessionInOrganization(sessionId, organizationId);
        if (!existsSessionInOrganization) {
            throw new PresentationSessionNotFoundException();
        }

        if (actor instanceof SessionActor.Member) {
            return;
        }

        if (sessionQueryApi.findActiveParticipantId(sessionId, actor).isEmpty()) {
            throw new PresentationAccessForbiddenException();
        }
    }
}
