package io.github.mongsil3344.qnow.presentation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.presentation.application.dto.PdfUrlResult;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationAccessForbiddenException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationNotFoundException;
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.domain.UploadStatus;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import java.util.Optional;
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
class PdfUrlServiceTest {

    private static final String BUCKET = "qnow-test-bucket";

    @Mock
    private PresentationRepository presentationRepository;

    @Mock
    private OrganizationQueryApi organizationQueryApi;

    @Mock
    private SessionQueryApi sessionQueryApi;

    private S3Presigner s3Presigner;
    private PdfUrlService pdfUrlService;

    @BeforeEach
    void setUp() {
        s3Presigner = S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")
                ))
                .build();
        pdfUrlService = new PdfUrlService(
                s3Presigner,
                presentationRepository,
                organizationQueryApi,
                sessionQueryApi,
                BUCKET,
                600
        );
    }

    @AfterEach
    void tearDown() {
        s3Presigner.close();
    }

    @Test
    void createPdfUrlReturnsPresignedGetUrl() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Presentation presentation = createUploadedPresentation(organizationId, sessionId, userId);

        when(organizationQueryApi.existsUserInOrganization(userId, organizationId)).thenReturn(true);
        when(sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)).thenReturn(true);
        when(presentationRepository.findByIdAndSessionIdAndUploadStatusAndDeletedAtIsNull(
                presentation.getId(),
                sessionId,
                UploadStatus.UPLOADED
        )).thenReturn(Optional.of(presentation));

        PdfUrlResult result = pdfUrlService.createPdfUrl(
                organizationId,
                sessionId,
                presentation.getId(),
                userId
        );

        assertThat(result.presentationId()).isEqualTo(presentation.getId());
        assertThat(result.pdfUrl()).contains("X-Amz-Signature");
        assertThat(result.pdfUrl()).contains("response-content-type=application%2Fpdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void createPdfUrlRejectsUserOutsideOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID presentationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(organizationQueryApi.existsUserInOrganization(userId, organizationId)).thenReturn(false);

        assertThatThrownBy(() -> pdfUrlService.createPdfUrl(
                organizationId,
                sessionId,
                presentationId,
                userId
        )).isInstanceOf(PresentationAccessForbiddenException.class);
    }

    @Test
    void createPdfUrlRejectsNonUploadedPresentation() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID presentationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(organizationQueryApi.existsUserInOrganization(userId, organizationId)).thenReturn(true);
        when(sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)).thenReturn(true);
        when(presentationRepository.findByIdAndSessionIdAndUploadStatusAndDeletedAtIsNull(
                presentationId,
                sessionId,
                UploadStatus.UPLOADED
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pdfUrlService.createPdfUrl(
                organizationId,
                sessionId,
                presentationId,
                userId
        )).isInstanceOf(PresentationNotFoundException.class);
    }

    private Presentation createUploadedPresentation(UUID organizationId, UUID sessionId, UUID presenterId) {
        Presentation presentation = Presentation.builder()
                .sessionId(sessionId)
                .presenterId(presenterId)
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
