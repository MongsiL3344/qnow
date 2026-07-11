package io.github.mongsil3344.qnow.presentation.application;

import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationDeleteForbiddenException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationNotFoundException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationSessionNotFoundException;
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.session.api.SessionStatusApi;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@Service
public class DeletePresentationService {

    private static final Logger log = LoggerFactory.getLogger(DeletePresentationService.class);

    private final PresentationRepository presentationRepository;
    private final OrganizationQueryApi organizationQueryApi;
    private final SessionQueryApi sessionQueryApi;
    private final SessionStatusApi sessionStatusApi;
    private final S3Client s3Client;
    private final String bucket;

    public DeletePresentationService(
            PresentationRepository presentationRepository,
            OrganizationQueryApi organizationQueryApi,
            SessionQueryApi sessionQueryApi,
            SessionStatusApi sessionStatusApi,
            S3Client s3Client,
            @Value("${qnow.storage.s3.bucket:}") String bucket
    ) {
        this.presentationRepository = presentationRepository;
        this.organizationQueryApi = organizationQueryApi;
        this.sessionQueryApi = sessionQueryApi;
        this.sessionStatusApi = sessionStatusApi;
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Transactional
    public void deletePresentation(UUID organizationId, UUID sessionId, UUID presentationId, UUID userId) {
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("S3 bucket is not configured");
        }

        validateDeletePermission(organizationId, sessionId, userId);

        Presentation presentation = presentationRepository.findByIdAndSessionIdAndDeletedAtIsNull(
                presentationId,
                sessionId
        ).orElseThrow(PresentationNotFoundException::new);

        if (!presentation.getPresenterId().equals(userId)) {
            throw new PresentationDeleteForbiddenException();
        }

        List<String> objectKeys = getObjectKeys(presentation);
        presentation.delete();
        deleteObjectsAfterCommit(objectKeys);
    }

    private void validateDeletePermission(UUID organizationId, UUID sessionId, UUID userId) {
        boolean existsUserInOrganization = organizationQueryApi.existsUserInOrganization(userId, organizationId);
        if (!existsUserInOrganization) {
            throw new PresentationDeleteForbiddenException();
        }

        boolean existsSessionInOrganization = sessionQueryApi.existsSessionInOrganization(sessionId, organizationId);
        if (!existsSessionInOrganization) {
            throw new PresentationSessionNotFoundException();
        }

        sessionStatusApi.requireNotEnded(sessionId);
    }

    private List<String> getObjectKeys(Presentation presentation) {
        List<String> objectKeys = new ArrayList<>();
        objectKeys.add(presentation.getS3Key());

        if (StringUtils.hasText(presentation.getThumbnailS3Key())) {
            objectKeys.add(presentation.getThumbnailS3Key());
        }

        return objectKeys;
    }

    private void deleteObjectsAfterCommit(List<String> objectKeys) {
        Runnable deleteObjects = () -> deleteObjects(objectKeys);

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteObjects.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteObjects.run();
            }
        });
    }

    private void deleteObjects(List<String> objectKeys) {
        for (String objectKey : objectKeys) {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .build());
            } catch (RuntimeException e) {
                log.warn("Failed to delete presentation object from S3. bucket={}, key={}", bucket, objectKey, e);
            }
        }
    }
}
