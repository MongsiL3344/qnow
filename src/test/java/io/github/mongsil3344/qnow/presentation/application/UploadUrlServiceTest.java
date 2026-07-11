package io.github.mongsil3344.qnow.presentation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.presentation.application.dto.UploadUrlResult;
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.session.api.SessionStatusApi;
import io.github.mongsil3344.qnow.session.api.SessionEndedException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ExtendWith(MockitoExtension.class)
class UploadUrlServiceTest {

    private static final String BUCKET = "qnow-test-bucket";

    @Mock
    private PresentationRepository presentationRepository;

    @Mock
    private OrganizationQueryApi organizationQueryApi;

    @Mock
    private SessionQueryApi sessionQueryApi;

    @Mock
    private SessionStatusApi sessionStatusApi;

    private S3Presigner s3Presigner;
    private UploadUrlService uploadUrlService;

    @BeforeEach
    void setUp() {
        s3Presigner = S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")
                ))
                .build();
        uploadUrlService = new UploadUrlService(
                s3Presigner,
                presentationRepository,
                organizationQueryApi,
                sessionQueryApi,
                sessionStatusApi,
                BUCKET,
                600
        );
    }

    @AfterEach
    void tearDown() {
        s3Presigner.close();
    }

    @Test
    void createUploadUrlReturnsOriginalAndThumbnailUploadTargets() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(organizationQueryApi.existsUserInOrganization(userId, organizationId)).thenReturn(true);
        when(sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)).thenReturn(true);

        UploadUrlResult result = uploadUrlService.createUploadUrl(
                organizationId,
                sessionId,
                userId,
                "Qnow 발표 자료",
                42
        );

        assertThat(result.objectKey()).endsWith("/original.pdf");
        assertThat(result.thumbnailObjectKey()).endsWith("/thumbnail.webp");
        assertThat(result.uploadUrl()).contains("X-Amz-Signature");
        assertThat(result.thumbnailUploadUrl()).contains("X-Amz-Signature");

        ArgumentCaptor<Presentation> presentationCaptor = ArgumentCaptor.forClass(Presentation.class);
        org.mockito.Mockito.verify(presentationRepository).save(presentationCaptor.capture());
        Presentation presentation = presentationCaptor.getValue();
        assertThat(presentation.getS3Key()).isEqualTo(result.objectKey());
        assertThat(presentation.getThumbnailS3Key()).isEqualTo(result.thumbnailObjectKey());
        assertThat(presentation.getPageCount()).isEqualTo(42);
    }

    @Test
    void createUploadUrlRejectsEndedSession() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(organizationQueryApi.existsUserInOrganization(userId, organizationId)).thenReturn(true);
        when(sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)).thenReturn(true);
        doThrow(new SessionEndedException()).when(sessionStatusApi).requireNotEnded(sessionId);

        assertThatThrownBy(() -> uploadUrlService.createUploadUrl(
            organizationId,
            sessionId,
            userId,
            "종료 세션 자료",
            10
        )).isInstanceOf(SessionEndedException.class);

        verifyNoInteractions(presentationRepository);
    }
}
