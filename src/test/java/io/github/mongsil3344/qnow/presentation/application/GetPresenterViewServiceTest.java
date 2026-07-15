package io.github.mongsil3344.qnow.presentation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.mongsil3344.qnow.presentation.application.dto.PresenterViewResult;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewClearReason;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewSnapshot;
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
class GetPresenterViewServiceTest {

    @Mock
    private PresenterViewAccessValidator accessValidator;

    @Mock
    private PresenterViewStateStore stateStore;

    @Mock
    private PresentationRepository presentationRepository;

    private GetPresenterViewService service;

    @BeforeEach
    void setUp() {
        service = new GetPresenterViewService(accessValidator, stateStore, presentationRepository);
    }

    @Test
    void 늦게_참여한_사용자는_현재_스냅샷과_제어_가능_여부를_조회한다() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SessionActor actor = new SessionActor.Member(userId);
        PresenterViewSnapshot empty = PresenterViewSnapshot.empty(sessionId);
        when(accessValidator.isSessionCreator(organizationId, sessionId, actor)).thenReturn(true);
        when(stateStore.get(sessionId)).thenReturn(empty);

        PresenterViewResult result = service.getPresenterView(organizationId, sessionId, userId);

        assertThat(result).isEqualTo(PresenterViewResult.from(true, empty));
        verifyNoInteractions(presentationRepository);
    }

    @Test
    void 삭제된_발표자료의_오래된_상태는_시퀀스가_있는_툼스톤으로_대체한다() {
        UUID organizationId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SessionActor actor = new SessionActor.Member(userId);
        UUID presentationId = UUID.randomUUID();
        PresenterViewSnapshot stale = new PresenterViewSnapshot(
            sessionId,
            presentationId,
            7,
            10,
            Instant.parse("2026-07-13T10:20:30Z")
        );
        PresenterViewSnapshot tombstone = new PresenterViewSnapshot(
            sessionId,
            null,
            null,
            11,
            Instant.parse("2026-07-13T10:21:00Z")
        );
        when(accessValidator.isSessionCreator(organizationId, sessionId, actor)).thenReturn(false);
        when(stateStore.get(sessionId)).thenReturn(stale);
        when(stateStore.clearPresentation(
            org.mockito.ArgumentMatchers.eq(sessionId),
            org.mockito.ArgumentMatchers.eq(presentationId),
            any(Instant.class),
            org.mockito.ArgumentMatchers.eq(PresenterViewClearReason.PRESENTATION_DELETED)
        )).thenReturn(Optional.of(tombstone));

        PresenterViewResult result = service.getPresenterView(organizationId, sessionId, userId);

        assertThat(result).isEqualTo(PresenterViewResult.from(false, tombstone));
        verify(stateStore).clearPresentation(
            org.mockito.ArgumentMatchers.eq(sessionId),
            org.mockito.ArgumentMatchers.eq(presentationId),
            any(Instant.class),
            org.mockito.ArgumentMatchers.eq(PresenterViewClearReason.PRESENTATION_DELETED)
        );
    }
}
