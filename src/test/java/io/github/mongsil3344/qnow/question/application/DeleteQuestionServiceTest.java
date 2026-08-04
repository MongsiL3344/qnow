package io.github.mongsil3344.qnow.question.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.mongsil3344.qnow.presentation.api.PresentationQueryApi;
import io.github.mongsil3344.qnow.presentation.api.UploadedPresentationInfo;
import io.github.mongsil3344.qnow.question.application.exception.QuestionDeleteForbiddenException;
import io.github.mongsil3344.qnow.question.application.exception.QuestionNotFoundException;
import io.github.mongsil3344.qnow.question.application.exception.SessionParticipantRequiredException;
import io.github.mongsil3344.qnow.question.domain.Question;
import io.github.mongsil3344.qnow.question.infrastructure.repo.QuestionRepository;
import io.github.mongsil3344.qnow.session.api.SessionAccessApi;
import io.github.mongsil3344.qnow.session.api.SessionActor;
import io.github.mongsil3344.qnow.session.api.SessionEndedException;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.session.api.SessionStatusApi;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteQuestionServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID PRESENTATION_ID = UUID.randomUUID();

    @Mock
    private PresentationQueryApi presentationQueryApi;

    @Mock
    private SessionQueryApi sessionQueryApi;

    @Mock
    private SessionStatusApi sessionStatusApi;

    @Mock
    private SessionAccessApi sessionAccessApi;

    @Mock
    private QuestionRepository questionRepository;

    private DeleteQuestionService deleteQuestionService;

    @BeforeEach
    void setUp() {
        deleteQuestionService = new DeleteQuestionService(
            presentationQueryApi,
            sessionQueryApi,
            sessionStatusApi,
            sessionAccessApi,
            questionRepository
        );
    }

    @Test
    void 작성자_회원이_자신의_질문을_삭제한다() {
        UUID userId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        SessionActor actor = new SessionActor.Member(userId);
        Question question = createQuestion(participantId);
        stubActiveQuestion(questionId, question, actor, participantId);
        stubMemberQuestioner(participantId, userId);

        deleteQuestionService.deleteQuestion(questionId, actor);

        assertThat(question.getDeletedAt()).isNotNull();
    }

    @Test
    void 재입장으로_참여자_ID가_바뀐_작성자_회원도_자신의_질문을_삭제할_수_있다() {
        UUID userId = UUID.randomUUID();
        UUID questionerParticipantId = UUID.randomUUID();
        UUID currentParticipantId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        SessionActor actor = new SessionActor.Member(userId);
        Question question = createQuestion(questionerParticipantId);
        stubActiveQuestion(questionId, question, actor, currentParticipantId);
        stubMemberQuestioner(questionerParticipantId, userId);

        deleteQuestionService.deleteQuestion(questionId, actor);

        assertThat(question.getDeletedAt()).isNotNull();
    }

    @Test
    void 세션_생성자가_다른_참여자의_질문을_삭제한다() {
        UUID creatorId = UUID.randomUUID();
        UUID questionerParticipantId = UUID.randomUUID();
        UUID questionerId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        SessionActor actor = new SessionActor.Member(creatorId);
        Question question = createQuestion(questionerParticipantId);
        stubActiveQuestion(questionId, question, actor, UUID.randomUUID());
        stubMemberQuestioner(questionerParticipantId, questionerId);
        when(sessionAccessApi.isSessionCreator(SESSION_ID, creatorId)).thenReturn(true);

        deleteQuestionService.deleteQuestion(questionId, actor);

        assertThat(question.getDeletedAt()).isNotNull();
        verify(sessionAccessApi).isSessionCreator(SESSION_ID, creatorId);
    }

    @Test
    void 작성자_게스트가_자신의_질문을_삭제한다() {
        UUID participantId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        SessionActor actor = new SessionActor.Guest(participantId, SESSION_ID);
        Question question = createQuestion(participantId);
        stubActiveQuestion(questionId, question, actor, participantId);

        deleteQuestionService.deleteQuestion(questionId, actor);

        assertThat(question.getDeletedAt()).isNotNull();
        verifyNoInteractions(sessionAccessApi);
    }

    @Test
    void 작성자도_생성자도_아닌_회원의_삭제를_거부한다() {
        UUID actorId = UUID.randomUUID();
        UUID questionerParticipantId = UUID.randomUUID();
        UUID questionerId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        SessionActor actor = new SessionActor.Member(actorId);
        Question question = createQuestion(questionerParticipantId);
        stubActiveQuestion(questionId, question, actor, UUID.randomUUID());
        stubMemberQuestioner(questionerParticipantId, questionerId);
        when(sessionAccessApi.isSessionCreator(SESSION_ID, actorId)).thenReturn(false);

        assertThatThrownBy(() -> deleteQuestionService.deleteQuestion(questionId, actor))
            .isInstanceOf(QuestionDeleteForbiddenException.class);

        assertThat(question.getDeletedAt()).isNull();
        verify(sessionAccessApi).isSessionCreator(SESSION_ID, actorId);
    }

    @Test
    void 다른_참여자의_질문을_게스트가_삭제할_수_없다() {
        UUID questionerParticipantId = UUID.randomUUID();
        UUID guestParticipantId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        SessionActor actor = new SessionActor.Guest(guestParticipantId, SESSION_ID);
        Question question = createQuestion(questionerParticipantId);
        stubActiveQuestion(questionId, question, actor, guestParticipantId);

        assertThatThrownBy(() -> deleteQuestionService.deleteQuestion(questionId, actor))
            .isInstanceOf(QuestionDeleteForbiddenException.class);

        assertThat(question.getDeletedAt()).isNull();
        verify(sessionAccessApi, never()).isSessionCreator(any(), any());
    }

    @Test
    void 존재하지_않는_질문의_삭제_요청을_거부한다() {
        UUID questionId = UUID.randomUUID();
        SessionActor actor = new SessionActor.Member(UUID.randomUUID());
        when(questionRepository.findActiveByIdForUpdate(questionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deleteQuestionService.deleteQuestion(questionId, actor))
            .isInstanceOf(QuestionNotFoundException.class);

        verifyNoInteractions(presentationQueryApi, sessionQueryApi, sessionStatusApi, sessionAccessApi);
    }

    @Test
    void 발표자료가_유효하지_않으면_질문을_찾을_수_없다() {
        UUID questionId = UUID.randomUUID();
        SessionActor actor = new SessionActor.Member(UUID.randomUUID());
        Question question = createQuestion(UUID.randomUUID());
        when(questionRepository.findActiveByIdForUpdate(questionId)).thenReturn(Optional.of(question));
        when(presentationQueryApi.findUploadedPresentationById(PRESENTATION_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> deleteQuestionService.deleteQuestion(questionId, actor))
            .isInstanceOf(QuestionNotFoundException.class);

        verifyNoInteractions(sessionQueryApi, sessionStatusApi, sessionAccessApi);
    }

    @Test
    void 세션이_존재하지_않으면_질문을_찾을_수_없다() {
        UUID questionId = UUID.randomUUID();
        SessionActor actor = new SessionActor.Member(UUID.randomUUID());
        Question question = createQuestion(UUID.randomUUID());
        when(questionRepository.findActiveByIdForUpdate(questionId)).thenReturn(Optional.of(question));
        when(presentationQueryApi.findUploadedPresentationById(PRESENTATION_ID))
            .thenReturn(Optional.of(uploadedPresentation()));
        when(sessionQueryApi.findOrganizationIdBySessionId(SESSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deleteQuestionService.deleteQuestion(questionId, actor))
            .isInstanceOf(QuestionNotFoundException.class);

        verifyNoInteractions(sessionStatusApi, sessionAccessApi);
    }

    @Test
    void 종료된_세션의_질문은_삭제할_수_없다() {
        UUID questionId = UUID.randomUUID();
        SessionActor actor = new SessionActor.Member(UUID.randomUUID());
        Question question = createQuestion(UUID.randomUUID());
        when(questionRepository.findActiveByIdForUpdate(questionId)).thenReturn(Optional.of(question));
        when(presentationQueryApi.findUploadedPresentationById(PRESENTATION_ID))
            .thenReturn(Optional.of(uploadedPresentation()));
        when(sessionQueryApi.findOrganizationIdBySessionId(SESSION_ID)).thenReturn(Optional.of(ORGANIZATION_ID));
        doThrow(new SessionEndedException()).when(sessionStatusApi).requireNotEnded(SESSION_ID);

        assertThatThrownBy(() -> deleteQuestionService.deleteQuestion(questionId, actor))
            .isInstanceOf(SessionEndedException.class);

        assertThat(question.getDeletedAt()).isNull();
        verifyNoInteractions(sessionAccessApi);
    }

    @Test
    void 활성_참여자가_아니면_질문을_삭제할_수_없다() {
        UUID questionId = UUID.randomUUID();
        SessionActor actor = new SessionActor.Member(UUID.randomUUID());
        Question question = createQuestion(UUID.randomUUID());
        when(questionRepository.findActiveByIdForUpdate(questionId)).thenReturn(Optional.of(question));
        when(presentationQueryApi.findUploadedPresentationById(PRESENTATION_ID))
            .thenReturn(Optional.of(uploadedPresentation()));
        when(sessionQueryApi.findOrganizationIdBySessionId(SESSION_ID)).thenReturn(Optional.of(ORGANIZATION_ID));
        when(sessionQueryApi.findActiveParticipantId(SESSION_ID, actor)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deleteQuestionService.deleteQuestion(questionId, actor))
            .isInstanceOf(SessionParticipantRequiredException.class);

        assertThat(question.getDeletedAt()).isNull();
        verifyNoInteractions(sessionAccessApi);
    }

    private void stubActiveQuestion(
        UUID questionId,
        Question question,
        SessionActor actor,
        UUID participantId
    ) {
        when(questionRepository.findActiveByIdForUpdate(questionId)).thenReturn(Optional.of(question));
        when(presentationQueryApi.findUploadedPresentationById(PRESENTATION_ID))
            .thenReturn(Optional.of(uploadedPresentation()));
        when(sessionQueryApi.findOrganizationIdBySessionId(SESSION_ID)).thenReturn(Optional.of(ORGANIZATION_ID));
        when(sessionQueryApi.findActiveParticipantId(SESSION_ID, actor)).thenReturn(Optional.of(participantId));
    }

    private void stubMemberQuestioner(UUID questionerParticipantId, UUID questionerUserId) {
        when(sessionQueryApi.findUserIdsByParticipantIds(Set.of(questionerParticipantId)))
            .thenReturn(Map.of(questionerParticipantId, questionerUserId));
    }

    private Question createQuestion(UUID questionerParticipantId) {
        return Question.builder()
            .presentationId(PRESENTATION_ID)
            .questionerId(questionerParticipantId)
            .content("삭제할 질문입니다")
            .anonymous(false)
            .pageStart(1)
            .pageEnd(1)
            .build();
    }

    private UploadedPresentationInfo uploadedPresentation() {
        return new UploadedPresentationInfo(PRESENTATION_ID, SESSION_ID, 10);
    }
}
