package io.github.mongsil3344.qnow.presentation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.mongsil3344.qnow.organization.api.OrganizationQueryApi;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationDeleteForbiddenException;
import io.github.mongsil3344.qnow.presentation.application.event.PresentationDeletedEvent;
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import io.github.mongsil3344.qnow.session.api.SessionEndedException;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.session.api.SessionStatusApi;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class DeletePresentationServiceTest {

    private static final String BUCKET = "qnow-test-bucket";

    @Mock
    private PresentationRepository presentationRepository;

    @Mock
    private OrganizationQueryApi organizationQueryApi;

    @Mock
    private SessionQueryApi sessionQueryApi;

    @Mock
    private SessionStatusApi sessionStatusApi;

    @Mock
    private S3Client s3Client;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DeletePresentationService deletePresentationService;

    @BeforeEach
    void setUp() {
        deletePresentationService = new DeletePresentationService(
                presentationRepository,
                organizationQueryApi,
                sessionQueryApi,
                sessionStatusApi,
                s3Client,
                eventPublisher,
                BUCKET
        );
    }

    @Test
    void 발표자료를_삭제하면_논리_삭제하고_S3_객체를_삭제한다() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID presenterId = UUID.randomUUID();
        Presentation presentation = createPresentation(organizationId, sessionId, presenterId);

        when(organizationQueryApi.existsUserInOrganization(presenterId, organizationId)).thenReturn(true);
        when(sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)).thenReturn(true);
        when(presentationRepository.findByIdAndSessionIdAndDeletedAtIsNull(presentation.getId(), sessionId))
                .thenReturn(Optional.of(presentation));

        deletePresentationService.deletePresentation(
                organizationId,
                sessionId,
                presentation.getId(),
                presenterId
        );

        assertThat(presentation.getDeletedAt()).isNotNull();
        verify(eventPublisher).publishEvent(new PresentationDeletedEvent(sessionId, presentation.getId()));

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, times(2)).deleteObject(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues())
                .extracting(DeleteObjectRequest::bucket)
                .containsExactly(BUCKET, BUCKET);
        assertThat(requestCaptor.getAllValues())
                .extracting(DeleteObjectRequest::key)
                .containsExactly(presentation.getS3Key(), presentation.getThumbnailS3Key());
    }

    @Test
    void 다른_발표자의_삭제_요청을_거부한다() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID presenterId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Presentation presentation = createPresentation(organizationId, sessionId, presenterId);

        when(organizationQueryApi.existsUserInOrganization(otherUserId, organizationId)).thenReturn(true);
        when(sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)).thenReturn(true);
        when(presentationRepository.findByIdAndSessionIdAndDeletedAtIsNull(presentation.getId(), sessionId))
                .thenReturn(Optional.of(presentation));

        assertThatThrownBy(() -> deletePresentationService.deletePresentation(
                organizationId,
                sessionId,
                presentation.getId(),
                otherUserId
        )).isInstanceOf(PresentationDeleteForbiddenException.class);

        assertThat(presentation.getDeletedAt()).isNull();
        verifyNoInteractions(s3Client);
    }

    @Test
    void 종료된_세션의_발표자료_삭제를_거부한다() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(organizationQueryApi.existsUserInOrganization(userId, organizationId)).thenReturn(true);
        when(sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)).thenReturn(true);
        doThrow(new SessionEndedException()).when(sessionStatusApi).requireNotEnded(sessionId);

        assertThatThrownBy(() -> deletePresentationService.deletePresentation(
            organizationId,
            sessionId,
            UUID.randomUUID(),
            userId
        )).isInstanceOf(SessionEndedException.class);

        verifyNoInteractions(presentationRepository, s3Client);
    }

    @Test
    void S3_삭제가_실패해도_논리_삭제_상태를_유지한다() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID presenterId = UUID.randomUUID();
        Presentation presentation = createPresentation(organizationId, sessionId, presenterId);

        when(organizationQueryApi.existsUserInOrganization(presenterId, organizationId)).thenReturn(true);
        when(sessionQueryApi.existsSessionInOrganization(sessionId, organizationId)).thenReturn(true);
        when(presentationRepository.findByIdAndSessionIdAndDeletedAtIsNull(presentation.getId(), sessionId))
                .thenReturn(Optional.of(presentation));
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(500).message("delete failed").build());

        deletePresentationService.deletePresentation(
                organizationId,
                sessionId,
                presentation.getId(),
                presenterId
        );

        assertThat(presentation.getDeletedAt()).isNotNull();
        verify(s3Client, times(2)).deleteObject(any(DeleteObjectRequest.class));
    }

    private Presentation createPresentation(UUID organizationId, UUID sessionId, UUID presenterId) {
        Presentation presentation = Presentation.builder()
                .sessionId(sessionId)
                .presenterId(presenterId)
                .title("Qnow 발표 자료")
                .pageCount(12)
                .build();
        String objectPrefix = "presentations/%s/%s/%s".formatted(
                organizationId,
                sessionId,
                presentation.getId()
        );
        presentation.assignS3Key("%s/original.pdf".formatted(objectPrefix));
        presentation.assignThumbnailS3Key("%s/thumbnail.webp".formatted(objectPrefix));
        presentation.setStatusUploaded();
        return presentation;
    }
}
