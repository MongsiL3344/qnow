package io.github.mongsil3344.qnow.question.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.mongsil3344.qnow.organization.domain.Organization;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.OrganizationRepository;
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import io.github.mongsil3344.qnow.question.domain.Question;
import io.github.mongsil3344.qnow.question.domain.QuestionSelection;
import io.github.mongsil3344.qnow.question.infrastructure.repo.QuestionRepository;
import io.github.mongsil3344.qnow.session.domain.Participant;
import io.github.mongsil3344.qnow.session.domain.Session;
import io.github.mongsil3344.qnow.session.infrastructure.repo.ParticipantRepository;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionRepository;
import io.github.mongsil3344.qnow.user.domain.User;
import io.github.mongsil3344.qnow.user.infrastructure.repo.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class QuestionControllerTest {

    private static final String PASSWORD = "password123";
    private static final int PAGE_COUNT = 12;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private PresentationRepository presentationRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createPageRangeQuestionPersistsAuthenticatedParticipantAndTrimmedContent() throws Exception {
        QuestionFixture fixture = createFixture(true, true);

        postQuestion(
                fixture,
                fixture.presentation().getId(),
                pageQuestionBody("  트랜잭션은 어느 시점에 커밋되나요?  ", 3, 5)
            )
            .andExpect(status().isCreated())
            .andExpect(content().string(""));

        Question question = findQuestion(fixture.presentation().getId());

        assertThat(question.getPresentationId()).isEqualTo(fixture.presentation().getId());
        assertThat(question.getQuestionerId()).isEqualTo(fixture.participant().getId());
        assertThat(question.getContent()).isEqualTo("트랜잭션은 어느 시점에 커밋되나요?");
        assertThat(question.isAnonymous()).isFalse();
        assertThat(question.getPageStart()).isEqualTo(3);
        assertThat(question.getPageEnd()).isEqualTo(5);
        assertThat(question.getUpvoteCount()).isZero();
        assertThat(question.getSelection()).isNull();
        assertThat(question.getCreatedAt()).isNotNull();
    }

    @Test
    void createQuestionPersistsAnonymousFlag() throws Exception {
        QuestionFixture fixture = createFixture(true, true);

        postQuestion(
                fixture,
                fixture.presentation().getId(),
                """
                    {
                      "content": "익명 질문입니다",
                      "anonymous": true,
                      "pageStart": 2,
                      "pageEnd": 2,
                      "selection": null
                    }
                    """
            )
            .andExpect(status().isCreated());

        assertThat(findQuestion(fixture.presentation().getId()).isAnonymous()).isTrue();
    }

    @Test
    void createQuestionRejectsEndedSession() throws Exception {
        QuestionFixture fixture = createFixture(true, true);
        Session session = sessionRepository.findById(fixture.presentation().getSessionId()).orElseThrow();
        session.end(Instant.parse("2026-06-17T11:00:00Z"));
        sessionRepository.saveAndFlush(session);

        postQuestion(
                fixture,
                fixture.presentation().getId(),
                pageQuestionBody("종료 후 질문", 1, 1)
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SESSION_ENDED"));

        assertThat(findQuestions(fixture.presentation().getId())).isEmpty();
    }

    @Test
    void createQuestionTreatsNullAnonymousAsFalse() throws Exception {
        QuestionFixture fixture = createFixture(true, true);

        postQuestion(
                fixture,
                fixture.presentation().getId(),
                """
                    {
                      "content": "익명 여부가 null인 질문입니다",
                      "anonymous": null,
                      "pageStart": 2,
                      "pageEnd": 2,
                      "selection": null
                    }
                    """
            )
            .andExpect(status().isCreated());

        assertThat(findQuestion(fixture.presentation().getId()).isAnonymous()).isFalse();
    }

    @Test
    void createAreaQuestionNormalizesRatiosAndPreservesExactBoundary() throws Exception {
        QuestionFixture fixture = createFixture(true, true);

        postQuestion(
                fixture,
                fixture.presentation().getId(),
                selectionQuestionBody(
                    "경계까지 선택한 영역 질문입니다",
                    7,
                    7,
                    "0.123456",
                    "0.200004",
                    "0.876544",
                    "0.799996"
                )
            )
            .andExpect(status().isCreated())
            .andExpect(content().string(""));

        QuestionSelection selection = findQuestion(fixture.presentation().getId()).getSelection();

        assertThat(selection).isNotNull();
        assertThat(selection.getLeftRatio()).isEqualByComparingTo(new BigDecimal("0.12346"));
        assertThat(selection.getTopRatio()).isEqualByComparingTo(new BigDecimal("0.20000"));
        assertThat(selection.getWidthRatio()).isEqualByComparingTo(new BigDecimal("0.87654"));
        assertThat(selection.getHeightRatio()).isEqualByComparingTo(new BigDecimal("0.80000"));
        assertThat(selection.getLeftRatio().add(selection.getWidthRatio()))
            .isEqualByComparingTo(BigDecimal.ONE);
        assertThat(selection.getTopRatio().add(selection.getHeightRatio()))
            .isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void createQuestionRejectsEmptySelectionObject() throws Exception {
        QuestionFixture fixture = createFixture(true, true);

        postQuestion(
                fixture,
                fixture.presentation().getId(),
                """
                    {
                      "content": "좌표가 없는 영역 질문",
                      "pageStart": 2,
                      "pageEnd": 2,
                      "selection": {}
                    }
                    """
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_QUESTION_REFERENCE"));

        assertThat(findQuestions(fixture.presentation().getId())).isEmpty();
    }

    @Test
    void createQuestionRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/presentations/{presentationId}/questions", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(pageQuestionBody("인증되지 않은 질문", 1, 1)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createQuestionRejectsNonexistentPresentation() throws Exception {
        QuestionFixture fixture = createFixture(true, true);

        postQuestion(fixture, UUID.randomUUID(), pageQuestionBody("존재하지 않는 자료 질문", 1, 1))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRESENTATION_NOT_FOUND"));
    }

    @Test
    void createQuestionRejectsPendingPresentation() throws Exception {
        QuestionFixture fixture = createFixture(false, true);

        postQuestion(
                fixture,
                fixture.presentation().getId(),
                pageQuestionBody("업로드 중인 자료 질문", 1, 1)
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRESENTATION_NOT_FOUND"));

        assertThat(findQuestions(fixture.presentation().getId())).isEmpty();
    }

    @Test
    void createQuestionRejectsDeletedPresentation() throws Exception {
        QuestionFixture fixture = createFixture(true, true);
        fixture.presentation().delete();
        presentationRepository.saveAndFlush(fixture.presentation());

        postQuestion(
                fixture,
                fixture.presentation().getId(),
                pageQuestionBody("삭제된 자료 질문", 1, 1)
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRESENTATION_NOT_FOUND"));

        assertThat(findQuestions(fixture.presentation().getId())).isEmpty();
    }

    @Test
    void createQuestionRejectsUserWhoIsNotActiveParticipant() throws Exception {
        QuestionFixture fixture = createFixture(true, false);

        postQuestion(
                fixture,
                fixture.presentation().getId(),
                pageQuestionBody("참여하지 않은 사용자의 질문", 1, 1)
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("SESSION_PARTICIPANT_REQUIRED"));

        assertThat(findQuestions(fixture.presentation().getId())).isEmpty();
    }

    @Test
    void createQuestionRejectsInvalidAndOutOfBoundsPageRanges() throws Exception {
        QuestionFixture fixture = createFixture(true, true);

        postQuestion(
                fixture,
                fixture.presentation().getId(),
                pageQuestionBody("0페이지 질문", 0, 1)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_QUESTION_REFERENCE"));

        postQuestion(
                fixture,
                fixture.presentation().getId(),
                pageQuestionBody("페이지 순서가 뒤집힌 질문", 5, 4)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_QUESTION_REFERENCE"));

        postQuestion(
                fixture,
                fixture.presentation().getId(),
                pageQuestionBody("자료 페이지를 벗어난 질문", 1, PAGE_COUNT + 1)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_QUESTION_REFERENCE"));

        assertThat(findQuestions(fixture.presentation().getId())).isEmpty();
    }

    @Test
    void createQuestionRejectsBlankAndTooLongContent() throws Exception {
        QuestionFixture fixture = createFixture(true, true);

        postQuestion(fixture, fixture.presentation().getId(), pageQuestionBody("   ", 1, 1))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        postQuestion(
                fixture,
                fixture.presentation().getId(),
                pageQuestionBody("a".repeat(501), 1, 1)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        assertThat(findQuestions(fixture.presentation().getId())).isEmpty();
    }

    @Test
    void createQuestionRejectsPartialOutOfBoundsAndMultiPageSelections() throws Exception {
        QuestionFixture fixture = createFixture(true, true);

        postQuestion(
                fixture,
                fixture.presentation().getId(),
                """
                    {
                      "content": "일부 좌표만 있는 질문",
                      "pageStart": 2,
                      "pageEnd": 2,
                      "selection": {
                        "leftRatio": 0.1,
                        "topRatio": 0.2,
                        "widthRatio": 0.3
                      }
                    }
                    """
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_QUESTION_REFERENCE"));

        postQuestion(
                fixture,
                fixture.presentation().getId(),
                selectionQuestionBody("오른쪽 경계를 벗어난 질문", 2, 2, "0.8", "0.2", "0.3", "0.4")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_QUESTION_REFERENCE"));

        postQuestion(
                fixture,
                fixture.presentation().getId(),
                selectionQuestionBody("여러 페이지에 영역이 있는 질문", 2, 3, "0.1", "0.2", "0.3", "0.4")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_QUESTION_REFERENCE"));

        assertThat(findQuestions(fixture.presentation().getId())).isEmpty();
    }

    private QuestionFixture createFixture(boolean uploaded, boolean activeParticipant) throws Exception {
        User user = saveUser();
        Organization organization = organizationRepository.save(Organization.builder()
            .name("org-" + UUID.randomUUID().toString().substring(0, 8))
            .detail("질문 등록 통합 테스트 그룹입니다.")
            .build());
        Session session = sessionRepository.save(Session.builder()
            .organizationId(organization.getId())
            .creatorId(user.getId())
            .title("question-session-" + UUID.randomUUID())
            .build());
        Participant participant = activeParticipant
            ? participantRepository.save(Participant.builder()
                .userId(user.getId())
                .session(session)
                .build())
            : null;
        Presentation presentation = Presentation.builder()
            .sessionId(session.getId())
            .presenterId(user.getId())
            .title("질문 등록 테스트 발표 자료")
            .pageCount(PAGE_COUNT)
            .build();
        presentation.assignS3Key("presentations/%s/%s/original.pdf".formatted(
            session.getId(),
            presentation.getId()
        ));
        if (uploaded) {
            presentation.setStatusUploaded();
        }
        presentation = presentationRepository.save(presentation);

        return new QuestionFixture(
            participant,
            presentation,
            login(user.getEmail(), PASSWORD)
        );
    }

    private User saveUser() {
        return userRepository.save(User.builder()
            .email("question-" + UUID.randomUUID() + "@example.com")
            .nickname("질문자")
            .password(passwordEncoder.encode(PASSWORD))
            .build());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "%s"
                    }
                    """.formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private ResultActions postQuestion(QuestionFixture fixture, UUID presentationId, String body) throws Exception {
        return mockMvc.perform(post("/presentations/{presentationId}/questions", presentationId)
            .session(fixture.loginSession())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
    }

    private Question findQuestion(UUID presentationId) {
        return findQuestions(presentationId).stream()
            .findFirst()
            .orElseThrow();
    }

    private List<Question> findQuestions(UUID presentationId) {
        return questionRepository.findAll().stream()
            .filter(question -> presentationId.equals(question.getPresentationId()))
            .toList();
    }

    private String pageQuestionBody(String content, int pageStart, int pageEnd) {
        return """
            {
              "content": "%s",
              "pageStart": %d,
              "pageEnd": %d,
              "selection": null
            }
            """.formatted(content, pageStart, pageEnd);
    }

    private String selectionQuestionBody(
            String content,
            int pageStart,
            int pageEnd,
            String left,
            String top,
            String width,
            String height
    ) {
        return """
            {
              "content": "%s",
              "pageStart": %d,
              "pageEnd": %d,
              "selection": {
                "leftRatio": %s,
                "topRatio": %s,
                "widthRatio": %s,
                "heightRatio": %s
              }
            }
            """.formatted(content, pageStart, pageEnd, left, top, width, height);
    }

    private record QuestionFixture(
            Participant participant,
            Presentation presentation,
            MockHttpSession loginSession
    ) {
    }
}
