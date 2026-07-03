package io.github.mongsil3344.qnow.presentation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.mongsil3344.qnow.presentation.api.SessionPresentationSummary;
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.domain.UploadStatus;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ExtendWith(MockitoExtension.class)
class PresentationQueryApiImplTest {

    private static final String BUCKET = "qnow-test-bucket";

    @Mock
    private PresentationRepository presentationRepository;

    private S3Presigner s3Presigner;
    private PresentationQueryApiImpl presentationQueryApi;

    @BeforeEach
    void setUp() {
        s3Presigner = S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")
                ))
                .build();
        presentationQueryApi = new PresentationQueryApiImpl(presentationRepository, s3Presigner, BUCKET, 600);
    }

    @AfterEach
    void tearDown() {
        s3Presigner.close();
    }

    @Test
    void findUploadedPresentationSummariesReturnsThumbnailUrlWhenThumbnailKeyExists() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Presentation presentation = createPresentation(organizationId, sessionId);
        presentation.assignThumbnailS3Key("%s/thumbnail.webp".formatted(presentation.getS3Key().replace("/original.pdf", "")));

        when(presentationRepository.findAllBySessionIdAndUploadStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                sessionId,
                UploadStatus.UPLOADED
        )).thenReturn(List.of(presentation));

        List<SessionPresentationSummary> result =
                presentationQueryApi.findUploadedPresentationSummariesBySessionId(sessionId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().thumbnailUrl()).contains("X-Amz-Signature");
    }

    @Test
    void findUploadedPresentationSummariesReturnsNullThumbnailUrlWhenThumbnailKeyDoesNotExist() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Presentation presentation = createPresentation(organizationId, sessionId);

        when(presentationRepository.findAllBySessionIdAndUploadStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                sessionId,
                UploadStatus.UPLOADED
        )).thenReturn(List.of(presentation));

        List<SessionPresentationSummary> result =
                presentationQueryApi.findUploadedPresentationSummariesBySessionId(sessionId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().thumbnailUrl()).isNull();
    }

    private Presentation createPresentation(UUID organizationId, UUID sessionId) {
        Presentation presentation = Presentation.builder()
                .sessionId(sessionId)
                .presenterId(UUID.randomUUID())
                .title("Qnow 발표 자료")
                .pageCount(12)
                .build();
        presentation.assignS3Key("presentations/%s/%s/%s/original.pdf".formatted(
                organizationId,
                sessionId,
                presentation.getId()
        ));
        presentation.setStatusUploaded();
        return presentation;
    }
}
