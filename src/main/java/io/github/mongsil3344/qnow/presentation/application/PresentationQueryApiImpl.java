package io.github.mongsil3344.qnow.presentation.application;

import io.github.mongsil3344.qnow.presentation.api.PresentationQueryApi;
import io.github.mongsil3344.qnow.presentation.api.SessionPresentationSummary;
import io.github.mongsil3344.qnow.presentation.api.UploadedPresentationInfo;
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.domain.UploadStatus;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Component
public class PresentationQueryApiImpl implements PresentationQueryApi {

    private final PresentationRepository presentationRepository;
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final Duration thumbnailUrlExpires;

    public PresentationQueryApiImpl(
        PresentationRepository presentationRepository,
        S3Presigner s3Presigner,
        @Value("${qnow.storage.s3.bucket:}") String bucket,
        @Value("${qnow.storage.s3.thumbnail-url-expires-seconds:600}") long thumbnailUrlExpiresSeconds
    ) {
        this.presentationRepository = presentationRepository;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
        this.thumbnailUrlExpires = Duration.ofSeconds(thumbnailUrlExpiresSeconds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionPresentationSummary> findUploadedPresentationSummariesBySessionId(UUID sessionId) {
        return presentationRepository
            .findAllBySessionIdAndUploadStatusAndDeletedAtIsNullOrderByCreatedAtDesc(sessionId, UploadStatus.UPLOADED)
            .stream()
            .map(presentation -> new SessionPresentationSummary(
                presentation.getId(),
                presentation.getTitle(),
                presentation.getPresenterId(),
                createThumbnailUrl(presentation)
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UploadedPresentationInfo> findUploadedPresentationById(UUID presentationId) {
        return presentationRepository
            .findByIdAndUploadStatusAndDeletedAtIsNull(presentationId, UploadStatus.UPLOADED)
            .map(presentation -> new UploadedPresentationInfo(
                presentation.getId(),
                presentation.getSessionId(),
                presentation.getPageCount()
            ));
    }

    private String createThumbnailUrl(Presentation presentation) {
        if (!StringUtils.hasText(bucket) || !StringUtils.hasText(presentation.getThumbnailS3Key())) {
            return null;
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(presentation.getThumbnailS3Key())
                .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(thumbnailUrlExpires)
                .getObjectRequest(getObjectRequest)
                .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
