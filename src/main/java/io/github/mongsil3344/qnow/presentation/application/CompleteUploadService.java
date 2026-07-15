package io.github.mongsil3344.qnow.presentation.application;

import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.presentation.application.exception.InvalidUploadObjectKeyException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationObjectNotFoundException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationUploadForbiddenException;
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class CompleteUploadService {

    private final PresentationRepository presentationRepository;
    private final OrganizationQueryApi organizationQueryApi;
    private final S3Client s3Client;
    private final String bucket;

    public CompleteUploadService(
            PresentationRepository presentationRepository,
            OrganizationQueryApi organizationQueryApi,
            S3Client s3Client,
            @Value("${qnow.storage.s3.bucket:}") String bucket
    ) {
        this.presentationRepository = presentationRepository;
        this.organizationQueryApi = organizationQueryApi;
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Transactional
    public void completeUpload(UUID organizationId, UUID sessionId, UUID userId, String objectKey) {
        if (!organizationQueryApi.existsUserInOrganization(userId, organizationId)) {
            throw new PresentationUploadForbiddenException();
        }

        String expectedPrefix = objectKeyPrefix(organizationId, sessionId);
        if (!objectKey.startsWith(expectedPrefix)) {
            throw new InvalidUploadObjectKeyException();
        }

        Presentation presentation = presentationRepository.findByS3KeyAndSessionIdAndDeletedAtIsNull(objectKey, sessionId)
                .orElseThrow(InvalidUploadObjectKeyException::new);
        verifyObjectExists(presentation.getS3Key());
        verifyThumbnailIfPresent(presentation);
        presentation.setStatusUploaded();
    }

    private void verifyThumbnailIfPresent(Presentation presentation) {
        if (!StringUtils.hasText(presentation.getThumbnailS3Key())) {
            return;
        }

        try {
            verifyObjectExists(presentation.getThumbnailS3Key());
        } catch (RuntimeException e) {
            presentation.clearThumbnailS3Key();
        }
    }

    private String objectKeyPrefix(UUID organizationId, UUID sessionId) {
        return "presentations/%s/%s/".formatted(organizationId, sessionId);
    }

    private void verifyObjectExists(String objectKey) {
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("S3 bucket is not configured");
        }

        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        try {
            s3Client.headObject(request);
        } catch (NoSuchKeyException e) {
            throw new PresentationObjectNotFoundException(e);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new PresentationObjectNotFoundException(e);
            }
            throw e;
        }
    }
}
