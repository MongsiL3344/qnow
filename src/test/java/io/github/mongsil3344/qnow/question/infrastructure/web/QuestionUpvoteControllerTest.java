package io.github.mongsil3344.qnow.question.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.mongsil3344.qnow.organization.domain.Organization;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.OrganizationRepository;
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import io.github.mongsil3344.qnow.question.domain.Question;
import io.github.mongsil3344.qnow.question.infrastructure.repo.QuestionRepository;
import io.github.mongsil3344.qnow.session.domain.Participant;
import io.github.mongsil3344.qnow.session.domain.Session;
import io.github.mongsil3344.qnow.session.infrastructure.repo.ParticipantRepository;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionRepository;
import io.github.mongsil3344.qnow.user.domain.User;
import io.github.mongsil3344.qnow.user.infrastructure.repo.UserRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class QuestionUpvoteControllerTest {

    private static final String PASSWORD = "password123";

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void upvoteCreatesOneVoteAndIsIdempotent() throws Exception {
        UpvoteFixture fixture = createFixture();

        putUpvote(fixture.question().getId(), fixture.voterLogin())
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.questionId").value(fixture.question().getId().toString()))
            .andExpect(jsonPath("$.upvotedByMe").value(true))
            .andExpect(jsonPath("$.upvoteCount").value(1));

        assertThat(upvoteRowCount(fixture.question().getId())).isEqualTo(1);
        assertThat(questionUpvoteCount(fixture.question().getId())).isEqualTo(1);

        putUpvote(fixture.question().getId(), fixture.voterLogin())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.questionId").value(fixture.question().getId().toString()))
            .andExpect(jsonPath("$.upvotedByMe").value(true))
            .andExpect(jsonPath("$.upvoteCount").value(1));

        assertThat(upvoteRowCount(fixture.question().getId())).isEqualTo(1);
        assertThat(questionUpvoteCount(fixture.question().getId())).isEqualTo(1);
    }

    @Test
    void cancelUpvoteHardDeletesVoteAndIsIdempotent() throws Exception {
        UpvoteFixture fixture = createFixture();
        putUpvote(fixture.question().getId(), fixture.voterLogin())
            .andExpect(status().isOk());

        deleteUpvote(fixture.question().getId(), fixture.voterLogin())
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.questionId").value(fixture.question().getId().toString()))
            .andExpect(jsonPath("$.upvotedByMe").value(false))
            .andExpect(jsonPath("$.upvoteCount").value(0));

        assertThat(upvoteRowCount(fixture.question().getId())).isZero();
        assertThat(questionUpvoteCount(fixture.question().getId())).isZero();

        deleteUpvote(fixture.question().getId(), fixture.voterLogin())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.upvotedByMe").value(false))
            .andExpect(jsonPath("$.upvoteCount").value(0));

        assertThat(upvoteRowCount(fixture.question().getId())).isZero();
        assertThat(questionUpvoteCount(fixture.question().getId())).isZero();
    }

    @Test
    void upvotesFromDifferentUsersIncrementCountWithoutLosingRows() throws Exception {
        UpvoteFixture fixture = createFixture();
        Voter secondVoter = saveVoter(fixture.presentationSession(), "두 번째 투표자");

        putUpvote(fixture.question().getId(), fixture.voterLogin())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.upvoteCount").value(1));
        putUpvote(fixture.question().getId(), secondVoter.loginSession())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.upvoteCount").value(2));

        assertThat(upvoteRowCount(fixture.question().getId())).isEqualTo(2);
        assertThat(questionUpvoteCount(fixture.question().getId())).isEqualTo(2);
        assertThat(voterUserIds(fixture.question().getId()))
            .containsExactlyInAnyOrder(fixture.voter().getId(), secondVoter.user().getId());
    }

    @Test
    void databaseRejectsDuplicateVoteForSameUser() throws Exception {
        UpvoteFixture fixture = createFixture();
        putUpvote(fixture.question().getId(), fixture.voterLogin())
            .andExpect(status().isOk());

        assertThatThrownBy(() -> insertRawUpvote(
            fixture.question().getId(),
            fixture.voter().getId()
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(upvoteRowCount(fixture.question().getId())).isEqualTo(1);
        assertThat(questionUpvoteCount(fixture.question().getId())).isEqualTo(1);
    }

    @Test
    void concurrentUpvotesPreserveVoteRowsAndQuestionCounter() throws Exception {
        UpvoteFixture fixture = createFixture();
        Voter secondVoter = saveVoter(fixture.presentationSession(), "동시 투표자");
        List<MockHttpSession> loginSessions = List.of(
            fixture.voterLogin(),
            secondVoter.loginSession()
        );
        ExecutorService executor = Executors.newFixedThreadPool(loginSessions.size());
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Integer>> responses = loginSessions.stream()
                .map(loginSession -> executor.submit(() -> {
                    start.await();
                    return mockMvc.perform(put("/questions/{questionId}/upvote", fixture.question().getId())
                            .session(loginSession))
                        .andReturn()
                        .getResponse()
                        .getStatus();
                }))
                .toList();

            start.countDown();

            for (Future<Integer> response : responses) {
                assertThat(response.get(10, TimeUnit.SECONDS)).isEqualTo(200);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(upvoteRowCount(fixture.question().getId())).isEqualTo(2);
        assertThat(questionUpvoteCount(fixture.question().getId())).isEqualTo(2);
    }

    @Test
    void upvoteRejectsQuestioner() throws Exception {
        UpvoteFixture fixture = createFixture();

        putUpvote(fixture.question().getId(), fixture.authorLogin())
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("SELF_UPVOTE_NOT_ALLOWED"));

        assertThat(upvoteRowCount(fixture.question().getId())).isZero();
        assertThat(questionUpvoteCount(fixture.question().getId())).isZero();
    }

    @Test
    void questionerCanCancelLegacySelfUpvoteAndHardDeleteIt() throws Exception {
        UpvoteFixture fixture = createFixture();
        insertRawUpvote(
            fixture.question().getId(),
            fixture.author().getId()
        );
        jdbcTemplate.update(
            "UPDATE questions SET upvote_count = 1 WHERE id = ?",
            fixture.question().getId()
        );

        deleteUpvote(fixture.question().getId(), fixture.authorLogin())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.upvotedByMe").value(false))
            .andExpect(jsonPath("$.upvoteCount").value(0));

        assertThat(upvoteRowCount(fixture.question().getId())).isZero();
        assertThat(questionUpvoteCount(fixture.question().getId())).isZero();
    }

    @Test
    void upvoteRejectsInactiveParticipant() throws Exception {
        UpvoteFixture fixture = createFixture();
        fixture.voterParticipant().exit();
        participantRepository.saveAndFlush(fixture.voterParticipant());

        putUpvote(fixture.question().getId(), fixture.voterLogin())
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("SESSION_PARTICIPANT_REQUIRED"));

        assertThat(upvoteRowCount(fixture.question().getId())).isZero();
        assertThat(questionUpvoteCount(fixture.question().getId())).isZero();
    }

    @Test
    void cancelUpvoteRejectsInactiveParticipantWithoutDeletingVote() throws Exception {
        UpvoteFixture fixture = createFixture();
        putUpvote(fixture.question().getId(), fixture.voterLogin())
            .andExpect(status().isOk());
        fixture.voterParticipant().exit();
        participantRepository.saveAndFlush(fixture.voterParticipant());

        deleteUpvote(fixture.question().getId(), fixture.voterLogin())
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("SESSION_PARTICIPANT_REQUIRED"));

        assertThat(upvoteRowCount(fixture.question().getId())).isEqualTo(1);
        assertThat(questionUpvoteCount(fixture.question().getId())).isEqualTo(1);
    }

    @Test
    void upvoteRequiresAuthentication() throws Exception {
        UpvoteFixture fixture = createFixture();

        mockMvc.perform(put("/questions/{questionId}/upvote", fixture.question().getId()))
            .andExpect(status().isUnauthorized());

        assertThat(upvoteRowCount(fixture.question().getId())).isZero();
        assertThat(questionUpvoteCount(fixture.question().getId())).isZero();
    }

    @Test
    void upvoteRejectsMissingAndDeletedQuestions() throws Exception {
        UpvoteFixture fixture = createFixture();
        UUID missingQuestionId = UUID.randomUUID();

        putUpvote(missingQuestionId, fixture.voterLogin())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"));
        deleteUpvote(missingQuestionId, fixture.voterLogin())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"));

        jdbcTemplate.update(
            "UPDATE questions SET deleted_at = ? WHERE id = ?",
            OffsetDateTime.now(ZoneOffset.UTC),
            fixture.question().getId()
        );

        putUpvote(fixture.question().getId(), fixture.voterLogin())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"));
    }

    @Test
    void upvoteRejectsQuestionWhosePresentationIsDeleted() throws Exception {
        UpvoteFixture fixture = createFixture();
        fixture.presentation().delete();
        presentationRepository.saveAndFlush(fixture.presentation());

        putUpvote(fixture.question().getId(), fixture.voterLogin())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"));

        assertThat(upvoteRowCount(fixture.question().getId())).isZero();
        assertThat(questionUpvoteCount(fixture.question().getId())).isZero();
    }

    @Test
    void upvoteRejectsMalformedQuestionId() throws Exception {
        UpvoteFixture fixture = createFixture();

        mockMvc.perform(put("/questions/not-a-uuid/upvote")
                .session(fixture.voterLogin()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    private UpvoteFixture createFixture() throws Exception {
        User author = saveUser("질문 작성자");
        User voter = saveUser("투표자");
        Organization organization = organizationRepository.save(Organization.builder()
            .name("upvote-" + UUID.randomUUID().toString().substring(0, 8))
            .detail("질문 공감 통합 테스트 그룹")
            .build());
        Session presentationSession = sessionRepository.save(Session.builder()
            .organizationId(organization.getId())
            .creatorId(author.getId())
            .title("질문 공감 테스트 세션")
            .build());
        Participant authorParticipant = saveParticipant(author, presentationSession);
        Participant voterParticipant = saveParticipant(voter, presentationSession);
        Presentation presentation = Presentation.builder()
            .sessionId(presentationSession.getId())
            .presenterId(author.getId())
            .title("질문 공감 테스트 발표")
            .pageCount(10)
            .build();
        presentation.assignS3Key("presentations/%s/%s/original.pdf".formatted(
            presentationSession.getId(),
            presentation.getId()
        ));
        presentation.setStatusUploaded();
        presentation = presentationRepository.save(presentation);
        Question question = questionRepository.saveAndFlush(Question.builder()
            .presentationId(presentation.getId())
            .questionerId(authorParticipant.getId())
            .content("좋아요를 받을 질문입니다")
            .anonymous(false)
            .pageStart(1)
            .pageEnd(1)
            .build());

        return new UpvoteFixture(
            author,
            voter,
            presentationSession,
            voterParticipant,
            presentation,
            question,
            login(author.getEmail()),
            login(voter.getEmail())
        );
    }

    private Voter saveVoter(Session presentationSession, String nickname) throws Exception {
        User user = saveUser(nickname);
        saveParticipant(user, presentationSession);
        return new Voter(user, login(user.getEmail()));
    }

    private User saveUser(String nickname) {
        return userRepository.save(User.builder()
            .email("question-upvote-" + UUID.randomUUID() + "@example.com")
            .nickname(nickname)
            .password(passwordEncoder.encode(PASSWORD))
            .build());
    }

    private Participant saveParticipant(User user, Session presentationSession) {
        return participantRepository.save(Participant.builder()
            .userId(user.getId())
            .session(presentationSession)
            .build());
    }

    private MockHttpSession login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "%s"
                    }
                    """.formatted(email, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private ResultActions putUpvote(UUID questionId, MockHttpSession loginSession) throws Exception {
        return mockMvc.perform(put("/questions/{questionId}/upvote", questionId)
            .session(loginSession));
    }

    private ResultActions deleteUpvote(UUID questionId, MockHttpSession loginSession) throws Exception {
        return mockMvc.perform(delete("/questions/{questionId}/upvote", questionId)
            .session(loginSession));
    }

    private long upvoteRowCount(UUID questionId) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM question_upvotes WHERE question_id = ?",
            Long.class,
            questionId
        );
    }

    private int questionUpvoteCount(UUID questionId) {
        return jdbcTemplate.queryForObject(
            "SELECT upvote_count FROM questions WHERE id = ?",
            Integer.class,
            questionId
        );
    }

    private List<UUID> voterUserIds(UUID questionId) {
        return jdbcTemplate.queryForList(
            "SELECT voter_user_id FROM question_upvotes WHERE question_id = ?",
            UUID.class,
            questionId
        );
    }

    private void insertRawUpvote(UUID questionId, UUID voterUserId) {
        jdbcTemplate.update(
            """
                INSERT INTO question_upvotes (id, question_id, voter_user_id, created_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                """,
            UUID.randomUUID(),
            questionId,
            voterUserId
        );
    }

    private record UpvoteFixture(
        User author,
        User voter,
        Session presentationSession,
        Participant voterParticipant,
        Presentation presentation,
        Question question,
        MockHttpSession authorLogin,
        MockHttpSession voterLogin
    ) {
    }

    private record Voter(User user, MockHttpSession loginSession) {
    }
}
