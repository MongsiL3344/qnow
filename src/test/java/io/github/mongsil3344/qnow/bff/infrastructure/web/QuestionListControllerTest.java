package io.github.mongsil3344.qnow.bff.infrastructure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.mongsil3344.qnow.organization.domain.Organization;
import io.github.mongsil3344.qnow.organization.domain.UserGroup;
import io.github.mongsil3344.qnow.organization.domain.UserGroupRole;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.OrganizationRepository;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.UserGroupRepository;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class QuestionListControllerTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

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
    void 활성_참여자가_아닌_조직_구성원도_질문을_조회할_수_있고_신원은_조건에_따라_가린다() throws Exception {
        QuestionFixture fixture = createFixture();

        mockMvc.perform(get("/presentations/{presentationId}/questions", fixture.presentationId())
                .session(fixture.loginSession()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(4))
            .andExpect(jsonPath("$.content[0].id").value(fixture.deletedUserQuestionId().toString()))
            .andExpect(jsonPath("$.content[0].questionerName").value("알 수 없는 사용자"))
            .andExpect(jsonPath("$.content[0].anonymous").value(false))
            .andExpect(jsonPath("$.content[0].mine").value(false))
            .andExpect(jsonPath("$.content[0].upvoteCount").value(1))
            .andExpect(jsonPath("$.content[0].upvotedByMe").value(false))
            .andExpect(jsonPath("$.content[1].id").value(fixture.anonymousMineQuestionId().toString()))
            .andExpect(jsonPath("$.content[1].questionerName").value("익명"))
            .andExpect(jsonPath("$.content[1].anonymous").value(true))
            .andExpect(jsonPath("$.content[1].mine").value(true))
            .andExpect(jsonPath("$.content[1].upvoteCount").value(5))
            .andExpect(jsonPath("$.content[1].upvotedByMe").value(false))
            .andExpect(jsonPath("$.content[2].id").value(fixture.exitedQuestionId().toString()))
            .andExpect(jsonPath("$.content[2].questionerName").value("퇴장 질문자"))
            .andExpect(jsonPath("$.content[2].upvoteCount").value(8))
            .andExpect(jsonPath("$.content[2].upvotedByMe").value(true))
            .andExpect(jsonPath("$.content[2].selection.leftRatio").value(0.1))
            .andExpect(jsonPath("$.content[2].selection.topRatio").value(0.2))
            .andExpect(jsonPath("$.content[2].selection.widthRatio").value(0.3))
            .andExpect(jsonPath("$.content[2].selection.heightRatio").value(0.4))
            .andExpect(jsonPath("$.content[3].upvoteCount").value(3))
            .andExpect(jsonPath("$.content[3].upvotedByMe").value(false))
            .andExpect(jsonPath("$.content[0].questionerId").doesNotExist())
            .andExpect(jsonPath("$.content[0].questionerParticipantId").doesNotExist())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void 질문_조회는_모든_정렬_방식과_슬라이스_페이징을_지원한다() throws Exception {
        QuestionFixture fixture = createFixture();

        expectQuestionOrder(
            getQuestions(fixture, "oldest", 0, 20),
            fixture.oldestQuestionId(),
            fixture.exitedQuestionId(),
            fixture.anonymousMineQuestionId(),
            fixture.deletedUserQuestionId()
        );
        expectQuestionOrder(
            getQuestions(fixture, "most_upvoted", 0, 20),
            fixture.exitedQuestionId(),
            fixture.anonymousMineQuestionId(),
            fixture.oldestQuestionId(),
            fixture.deletedUserQuestionId()
        );
        expectQuestionOrder(
            getQuestions(fixture, "page_start_asc", 0, 20),
            fixture.deletedUserQuestionId(),
            fixture.exitedQuestionId(),
            fixture.anonymousMineQuestionId(),
            fixture.oldestQuestionId()
        );

        getQuestions(fixture, "oldest", 0, 2)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(fixture.oldestQuestionId().toString()))
            .andExpect(jsonPath("$.content[1].id").value(fixture.exitedQuestionId().toString()))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.hasNext").value(true));

        getQuestions(fixture, "oldest", 1, 2)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(fixture.anonymousMineQuestionId().toString()))
            .andExpect(jsonPath("$.content[1].id").value(fixture.deletedUserQuestionId().toString()))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void 질문_조회는_유효하지_않은_쿼리_값을_거부한다() throws Exception {
        QuestionFixture fixture = createFixture();

        getQuestions(fixture, "popular", 0, 20)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        getQuestions(fixture, "latest", -1, 20)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        getQuestions(fixture, "latest", 0, 0)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        getQuestions(fixture, "latest", 0, 101)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mockMvc.perform(get("/presentations/{presentationId}/questions", fixture.presentationId())
                .session(fixture.loginSession())
                .param("page", "not-a-number"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void 질문_조회는_인증과_조직_가입이_필요하다() throws Exception {
        QuestionFixture fixture = createFixture();

        mockMvc.perform(get("/presentations/{presentationId}/questions", fixture.presentationId()))
            .andExpect(status().isUnauthorized());

        User outsider = saveUser("외부 사용자");
        MockHttpSession outsiderSession = login(outsider.getEmail());

        mockMvc.perform(get("/presentations/{presentationId}/questions", fixture.presentationId())
                .session(outsiderSession))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ORGANIZATION_MEMBER_REQUIRED"));

        mockMvc.perform(get("/presentations/{presentationId}/questions", UUID.randomUUID())
                .session(fixture.loginSession()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRESENTATION_NOT_FOUND"));
    }

    private QuestionFixture createFixture() throws Exception {
        User currentUser = saveUser("현재 사용자");
        User oldestQuestioner = saveUser("오래된 질문자");
        User exitedQuestioner = saveUser("퇴장 질문자");
        User deletedQuestioner = saveUser("삭제된 질문자");
        Organization organization = organizationRepository.save(Organization.builder()
            .name("question-list-" + UUID.randomUUID().toString().substring(0, 8))
            .detail("질문 목록 통합 테스트 그룹")
            .build());

        userGroupRepository.saveAll(List.of(
            membership(currentUser, organization, UserGroupRole.ADMIN),
            membership(oldestQuestioner, organization, UserGroupRole.USER),
            membership(exitedQuestioner, organization, UserGroupRole.USER),
            membership(deletedQuestioner, organization, UserGroupRole.USER)
        ));

        Session session = sessionRepository.save(Session.builder()
            .organizationId(organization.getId())
            .creatorId(currentUser.getId())
            .title("질문 목록 테스트 세션")
            .build());
        Participant currentParticipant = saveParticipant(currentUser, session);
        Participant oldestParticipant = saveParticipant(oldestQuestioner, session);
        Participant exitedParticipant = saveParticipant(exitedQuestioner, session);
        Participant deletedUserParticipant = saveParticipant(deletedQuestioner, session);
        exitedParticipant.exit();
        participantRepository.saveAndFlush(exitedParticipant);

        Presentation presentation = Presentation.builder()
            .sessionId(session.getId())
            .presenterId(currentUser.getId())
            .title("질문 목록 테스트 발표")
            .pageCount(10)
            .build();
        presentation.assignS3Key("presentations/%s/%s/original.pdf".formatted(
            session.getId(),
            presentation.getId()
        ));
        presentation.setStatusUploaded();
        presentation = presentationRepository.save(presentation);

        Instant baseTime = Instant.parse("2026-01-01T00:00:00Z");
        UUID oldestQuestionId = saveQuestion(
            presentation,
            oldestParticipant,
            "가장 오래된 질문",
            false,
            4,
            null,
            3,
            baseTime
        );
        UUID exitedQuestionId = saveQuestion(
            presentation,
            exitedParticipant,
            "퇴장한 참여자의 질문",
            false,
            2,
            new QuestionSelection(
                new BigDecimal("0.10000"),
                new BigDecimal("0.20000"),
                new BigDecimal("0.30000"),
                new BigDecimal("0.40000")
            ),
            8,
            baseTime.plusSeconds(1)
        );
        UUID anonymousMineQuestionId = saveQuestion(
            presentation,
            currentParticipant,
            "발표자의 익명 질문",
            true,
            3,
            null,
            5,
            baseTime.plusSeconds(2)
        );
        UUID deletedUserQuestionId = saveQuestion(
            presentation,
            deletedUserParticipant,
            "삭제된 사용자의 질문",
            false,
            1,
            null,
            1,
            baseTime.plusSeconds(3)
        );
        UUID softDeletedQuestionId = saveQuestion(
            presentation,
            oldestParticipant,
            "삭제된 질문",
            false,
            5,
            null,
            100,
            baseTime.plusSeconds(4)
        );

        insertQuestionUpvote(
            exitedQuestionId,
            currentUser.getId(),
            baseTime.plusSeconds(5)
        );

        currentParticipant.exit();
        participantRepository.saveAndFlush(currentParticipant);

        jdbcTemplate.update(
            "UPDATE users SET deleted_at = ? WHERE id = ?",
            OffsetDateTime.ofInstant(baseTime.plusSeconds(5), ZoneOffset.UTC),
            deletedQuestioner.getId()
        );
        jdbcTemplate.update(
            "UPDATE questions SET deleted_at = ? WHERE id = ?",
            OffsetDateTime.ofInstant(baseTime.plusSeconds(5), ZoneOffset.UTC),
            softDeletedQuestionId
        );

        return new QuestionFixture(
            presentation.getId(),
            login(currentUser.getEmail()),
            oldestQuestionId,
            exitedQuestionId,
            anonymousMineQuestionId,
            deletedUserQuestionId
        );
    }

    private User saveUser(String nickname) {
        return userRepository.save(User.builder()
            .email("question-list-" + UUID.randomUUID() + "@example.com")
            .nickname(nickname)
            .password(passwordEncoder.encode(PASSWORD))
            .build());
    }

    private UserGroup membership(User user, Organization organization, UserGroupRole role) {
        return UserGroup.builder()
            .userId(user.getId())
            .organization(organization)
            .role(role)
            .build();
    }

    private Participant saveParticipant(User user, Session session) {
        return participantRepository.save(Participant.member(user.getId(), session));
    }

    private UUID saveQuestion(
        Presentation presentation,
        Participant participant,
        String content,
        boolean anonymous,
        int page,
        QuestionSelection selection,
        int upvoteCount,
        Instant createdAt
    ) {
        Question question = questionRepository.saveAndFlush(Question.builder()
            .presentationId(presentation.getId())
            .questionerId(participant.getId())
            .content(content)
            .anonymous(anonymous)
            .pageStart(page)
            .pageEnd(page)
            .selection(selection)
            .build());

        jdbcTemplate.update(
            "UPDATE questions SET upvote_count = ?, created_at = ? WHERE id = ?",
            upvoteCount,
            OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC),
            question.getId()
        );

        return question.getId();
    }

    private void insertQuestionUpvote(
        UUID questionId,
        UUID voterUserId,
        Instant createdAt
    ) {
        jdbcTemplate.update(
            """
                INSERT INTO question_upvotes (id, question_id, voter_user_id, created_at)
                VALUES (?, ?, ?, ?)
                """,
            UUID.randomUUID(),
            questionId,
            voterUserId,
            OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC)
        );
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

    private ResultActions getQuestions(QuestionFixture fixture, String sort, int page, int size) throws Exception {
        return mockMvc.perform(get("/presentations/{presentationId}/questions", fixture.presentationId())
            .session(fixture.loginSession())
            .param("sort", sort)
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size)));
    }

    private void expectQuestionOrder(ResultActions result, UUID... questionIds) throws Exception {
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(questionIds.length));

        for (int index = 0; index < questionIds.length; index++) {
            result.andExpect(jsonPath("$.content[%d].id".formatted(index)).value(questionIds[index].toString()));
        }
    }

    private record QuestionFixture(
        UUID presentationId,
        MockHttpSession loginSession,
        UUID oldestQuestionId,
        UUID exitedQuestionId,
        UUID anonymousMineQuestionId,
        UUID deletedUserQuestionId
    ) {
    }
}
