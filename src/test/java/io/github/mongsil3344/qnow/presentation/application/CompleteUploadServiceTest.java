package io.github.mongsil3344.qnow.presentation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.mongsil3344.qnow.presentation.application.exception.PresentationObjectNotFoundException;
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.domain.UploadStatus;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class CompleteUploadServiceTest {

    private static final String BUCKET = "qnow-test-bucket";

    @Mock
    private PresentationRepository presentationRepository;

    @Mock
    private S3Client s3Client;

    private CompleteUploadService completeUploadService;

    @BeforeEach
    void setUp() {
        completeUploadService = new CompleteUploadService(presentationRepository, s3Client, BUCKET);
    }

    @Test
    void completeUploadChecksS3ObjectBeforeMarkingUploaded() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Presentation presentation = createPresentation(organizationId, sessionId);

        when(presentationRepository.findByS3KeyAndSessionIdAndDeletedAtIsNull(presentation.getS3Key(), sessionId))
                .thenReturn(Optional.of(presentation));
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        completeUploadService.completeUpload(organizationId, sessionId, presentation.getS3Key());

        assertThat(presentation.getUploadStatus()).isEqualTo(UploadStatus.UPLOADED);

        ArgumentCaptor<HeadObjectRequest> requestCaptor = ArgumentCaptor.forClass(HeadObjectRequest.class);
        verify(s3Client, times(2)).headObject(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues())
                .extracting(HeadObjectRequest::bucket)
                .containsExactly(BUCKET, BUCKET);
        assertThat(requestCaptor.getAllValues())
                .extracting(HeadObjectRequest::key)
                .containsExactly(presentation.getS3Key(), presentation.getThumbnailS3Key());
        assertThat(presentation.getThumbnailS3Key()).endsWith("/thumbnail.webp");
    }

    @Test
    void completeUploadDoesNotMarkUploadedWhenS3ObjectDoesNotExist() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Presentation presentation = createPresentation(organizationId, sessionId);

        when(presentationRepository.findByS3KeyAndSessionIdAndDeletedAtIsNull(presentation.getS3Key(), sessionId))
                .thenReturn(Optional.of(presentation));
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).message("not found").build());

        assertThatThrownBy(() -> completeUploadService.completeUpload(organizationId, sessionId, presentation.getS3Key()))
                .isInstanceOf(PresentationObjectNotFoundException.class);

        assertThat(presentation.getUploadStatus()).isEqualTo(UploadStatus.PENDING);
        assertThat(presentation.getThumbnailS3Key()).endsWith("/thumbnail.webp");
    }

    @Test
    void completeUploadClearsThumbnailKeyAndMarksUploadedWhenThumbnailDoesNotExist() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Presentation presentation = createPresentation(organizationId, sessionId);

        when(presentationRepository.findByS3KeyAndSessionIdAndDeletedAtIsNull(presentation.getS3Key(), sessionId))
                .thenReturn(Optional.of(presentation));
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build())
                .thenThrow(S3Exception.builder().statusCode(404).message("not found").build());

        completeUploadService.completeUpload(organizationId, sessionId, presentation.getS3Key());

        assertThat(presentation.getUploadStatus()).isEqualTo(UploadStatus.UPLOADED);
        assertThat(presentation.getThumbnailS3Key()).isNull();
    }

    private Presentation createPresentation(UUID organizationId, UUID sessionId) {
        Presentation presentation = Presentation.builder()
                .sessionId(sessionId)
                .presenterId(UUID.randomUUID())
                .title("Qnow 발표 자료")
                .build();
        String objectPrefix = "presentations/%s/%s/%s".formatted(
                organizationId,
                sessionId,
                presentation.getId()
        );
        presentation.assignS3Key("%s/original.pdf".formatted(objectPrefix));
        presentation.assignThumbnailS3Key("%s/thumbnail.webp".formatted(objectPrefix));
        return presentation;
    }
}
