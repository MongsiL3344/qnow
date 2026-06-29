package io.github.mongsil3344.qnow.presentation.application;

import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.presentation.application.dto.UploadUrlResult;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationSessionNotFoundException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationUploadForbiddenException;
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class UploadUrlService {

    private final S3Presigner s3Presigner;
    private final PresentationRepository presentationRepository;
    private final OrganizationQueryApi organizationQueryApi;
    private final SessionQueryApi sessionQueryApi;
    private final String bucket;
    private final Duration uploadUrlExpires;

    public UploadUrlService(
            S3Presigner s3Presigner,
            PresentationRepository presentationRepository,
            OrganizationQueryApi organizationQueryApi,
            SessionQueryApi sessionQueryApi,
            @Value("${qnow.storage.s3.bucket:}") String bucket,
            @Value("${qnow.storage.s3.upload-url-expires-seconds:600}") long uploadUrlExpiresSeconds
    ) {
        this.s3Presigner = s3Presigner;
        this.presentationRepository = presentationRepository;
        this.organizationQueryApi = organizationQueryApi;
        this.sessionQueryApi = sessionQueryApi;
        this.bucket = bucket;
        this.uploadUrlExpires = Duration.ofSeconds(uploadUrlExpiresSeconds);
    }

    @Transactional
    public UploadUrlResult createUploadUrl(UUID organizationId, UUID sessionId, UUID userId, String title) {
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("S3 bucket is not configured");
        }

        validateUploadPermission(organizationId, sessionId, userId);

        Presentation presentation = Presentation.builder()
                .sessionId(sessionId)
                .presenterId(userId)
                .title(title)
                .build();

        String objectKey = createObjectKey(organizationId, sessionId, presentation.getId());
        presentation.assignS3Key(objectKey);
        presentationRepository.save(presentation);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(uploadUrlExpires)
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        URL uploadUrl = presignedRequest.url();

        return new UploadUrlResult(
                presentation.getId(),
                uploadUrl.toString(),
                objectKey,
                Instant.now().plus(uploadUrlExpires)
        );
    }

    /* 업로드 권한 확인 메서드 */
    private void validateUploadPermission(UUID organizationId, UUID sessionId, UUID userId) {
        // 유저가 조직에 있는지 검사
        boolean existsUserInOrganization = organizationQueryApi.existsUserInOrganization(userId, organizationId);
        if (!existsUserInOrganization) {
            throw new PresentationUploadForbiddenException();
        }

        // 실제로 조직에 있는 세션인지 검사
        boolean existsSessionInOrganization = sessionQueryApi.existsSessionInOrganization(sessionId, organizationId);
        if (!existsSessionInOrganization) {
            throw new PresentationSessionNotFoundException();
        }
    }

    /* S3 객체 Key 생성 메서드 */
    private String createObjectKey(UUID organizationId, UUID sessionId, UUID presentationId) {
        return "presentations/%s/%s/%s".formatted(organizationId, sessionId, presentationId);
    }
}
