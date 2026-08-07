package io.github.mongsil3344.qnow.presentation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.mongsil3344.qnow.presentation.application.dto.PresenterViewResult;
import io.github.mongsil3344.qnow.presentation.application.exception.InvalidPresenterViewPageException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresentationNotFoundException;
import io.github.mongsil3344.qnow.presentation.application.exception.PresenterViewControlForbiddenException;
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewSnapshot;
import io.github.mongsil3344.qnow.presentation.domain.UploadStatus;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdatePresenterViewServiceTest {

    @Mock
    private PresenterViewAccessValidator accessValidator;

    @Mock
    private PresenterViewStateStore stateStore;

    @Mock
    private PresentationRepository presentationRepository;

    private UpdatePresenterViewService service;

    @BeforeEach
    void setUp() {
        service = new UpdatePresenterViewService(accessValidator, stateStore, presentationRepository);
    }

    @Test
    void 생성자는_발표자_화면을_변경할_수_있다() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Presentation presentation = createPresentation(sessionId, userId, 12);
        PresenterViewSnapshot expected = new PresenterViewSnapshot(
            sessionId,
            presentation.getId(),
            5,
            1,
            Instant.parse("2026-07-13T10:20:30Z")
        );
        SessionActor actor = new SessionActor.Member(userId);
        when(accessValidator.getControlStatus(organizationId, sessionId, actor)).thenReturn(
            new PresenterViewAccessValidator.PresenterControlStatus(true, true, null, UUID.randomUUID())
        );
        when(presentationRepository.findByIdAndSessionIdAndUploadStatusAndDeletedAtIsNull(
            presentation.getId(),
            sessionId,
            UploadStatus.UPLOADED
        )).thenReturn(Optional.of(presentation));
        when(stateStore.update(
            org.mockito.ArgumentMatchers.eq(sessionId),
            org.mockito.ArgumentMatchers.eq(presentation.getId()),
            org.mockito.ArgumentMatchers.eq(5),
            any(Instant.class)
        ))
            .thenReturn(expected);

        assertThat(service.updatePresenterView(
            organizationId,
            sessionId,
            actor,
            presentation.getId(),
            5
        )).isEqualTo(PresenterViewResult.from(true, expected));
    }

    @Test
    void 생성자가_아닌_사용자는_발표자_화면을_변경할_수_없다() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SessionActor actor = new SessionActor.Member(userId);
        when(accessValidator.getControlStatus(organizationId, sessionId, actor)).thenReturn(
            new PresenterViewAccessValidator.PresenterControlStatus(false, false, null, UUID.randomUUID())
        );

        assertThatThrownBy(() -> service.updatePresenterView(
            organizationId,
            sessionId,
            actor,
            UUID.randomUUID(),
            1
        )).isInstanceOf(PresenterViewControlForbiddenException.class);
        verifyNoInteractions(stateStore, presentationRepository);
    }

    @Test
    void 다른_세션의_발표자료는_거부한다() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID presentationId = UUID.randomUUID();
        SessionActor actor = new SessionActor.Member(userId);
        when(accessValidator.getControlStatus(organizationId, sessionId, actor)).thenReturn(
            new PresenterViewAccessValidator.PresenterControlStatus(true, true, null, UUID.randomUUID())
        );

        assertThatThrownBy(() -> service.updatePresenterView(
            organizationId,
            sessionId,
            actor,
            presentationId,
            1
        )).isInstanceOf(PresentationNotFoundException.class);

        verifyNoInteractions(stateStore);
    }

    @Test
    void 발표자료_페이지_범위를_벗어난_페이지는_거부한다() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Presentation presentation = createPresentation(sessionId, userId, 12);
        SessionActor actor = new SessionActor.Member(userId);
        when(accessValidator.getControlStatus(organizationId, sessionId, actor)).thenReturn(
            new PresenterViewAccessValidator.PresenterControlStatus(true, true, null, UUID.randomUUID())
        );
        when(presentationRepository.findByIdAndSessionIdAndUploadStatusAndDeletedAtIsNull(
            presentation.getId(),
            sessionId,
            UploadStatus.UPLOADED
        )).thenReturn(Optional.of(presentation));

        assertThatThrownBy(() -> service.updatePresenterView(
            organizationId,
            sessionId,
            actor,
            presentation.getId(),
            13
        )).isInstanceOf(InvalidPresenterViewPageException.class);

        verifyNoInteractions(stateStore);
    }

    @Test
    void 같은_위치로_변경하면_기존_시퀀스를_반환한다() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Presentation presentation = createPresentation(sessionId, userId, 12);
        PresenterViewSnapshot existing = new PresenterViewSnapshot(
            sessionId,
            presentation.getId(),
            3,
            9,
            Instant.parse("2026-07-13T10:20:30Z")
        );
        SessionActor actor = new SessionActor.Member(userId);
        when(accessValidator.getControlStatus(organizationId, sessionId, actor)).thenReturn(
            new PresenterViewAccessValidator.PresenterControlStatus(true, true, null, UUID.randomUUID())
        );
        when(presentationRepository.findByIdAndSessionIdAndUploadStatusAndDeletedAtIsNull(
            presentation.getId(),
            sessionId,
            UploadStatus.UPLOADED
        )).thenReturn(Optional.of(presentation));
        when(stateStore.update(
            org.mockito.ArgumentMatchers.eq(sessionId),
            org.mockito.ArgumentMatchers.eq(presentation.getId()),
            org.mockito.ArgumentMatchers.eq(3),
            any(Instant.class)
        ))
            .thenReturn(existing);

        assertThat(service.updatePresenterView(
            organizationId,
            sessionId,
            actor,
            presentation.getId(),
            3
        ).sequence()).isEqualTo(9);
    }

    private Presentation createPresentation(UUID sessionId, UUID presenterId, int pageCount) {
        Presentation presentation = Presentation.builder()
            .sessionId(sessionId)
            .presenterId(presenterId)
            .title("Qnow 발표 자료")
            .pageCount(pageCount)
            .build();
        presentation.assignS3Key("presentations/%s/%s/original.pdf".formatted(sessionId, presentation.getId()));
        presentation.setStatusUploaded();
        return presentation;
    }
}
