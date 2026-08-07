package io.github.mongsil3344.qnow.question.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import io.github.mongsil3344.qnow.question.infrastructure.repo.QuestionRepository;
import io.github.mongsil3344.qnow.session.api.GuestPrincipal;
import io.github.mongsil3344.qnow.session.domain.Participant;
import io.github.mongsil3344.qnow.session.domain.Session;
import io.github.mongsil3344.qnow.session.infrastructure.repo.ParticipantRepository;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionRepository;
import io.github.mongsil3344.qnow.user.domain.User;
import io.github.mongsil3344.qnow.user.infrastructure.repo.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class QuestionDeleteControllerTest {

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
    void 작성자가_자신의_질문을_삭제하면_204를_반환하고_목록에서_제외한다() throws Exception {
        DeleteFixture fixture = createFixture(true);

        getQuestions(fixture.presentation().getId(), fixture.creatorLogin())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1));

        deleteQuestion(fixture.question().getId(), fixture.creatorLogin())
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));

        assertThat(deletedAt(fixture.question().getId())).isNotNull();
        getQuestions(fixture.presentation().getId(), fixture.creatorLogin())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void 세션_생성자가_다른_참여자의_질문을_삭제한다() throws Exception {
        DeleteFixture fixture = createFixture(false);

        deleteQuestion(fixture.question().getId(), fixture.creatorLogin())
            .andExpect(status().isNoContent());

        assertThat(deletedAt(fixture.question().getId())).isNotNull();
    }

    @Test
    void 게스트_작성자가_자신의_질문을_삭제한다() throws Exception {
        DeleteFixture fixture = createGuestQuestionFixture();

        deleteGuestQuestion(
                fixture.question().getId(),
                fixture.questionerParticipant(),
                fixture.presentationSession()
            )
            .andExpect(status().isNoContent());

        assertThat(deletedAt(fixture.question().getId())).isNotNull();
    }

    @Test
    void 작성자도_생성자도_아닌_참여자의_삭제_요청은_403을_반환한다() throws Exception {
        DeleteFixture fixture = createFixture(true);

        deleteQuestion(fixture.question().getId(), fixture.memberLogin())
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("QUESTION_DELETE_FORBIDDEN"));

        assertThat(deletedAt(fixture.question().getId())).isNull();
    }

    @Test
    void 게스트는_다른_참여자의_질문을_삭제할_수_없다() throws Exception {
        DeleteFixture fixture = createFixture(true);
        Participant guest = participantRepository.saveAndFlush(
            Participant.guest("삭제 시도 손님", fixture.presentationSession())
        );

        deleteGuestQuestion(
                fixture.question().getId(),
                guest,
                fixture.presentationSession()
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("QUESTION_DELETE_FORBIDDEN"));

        assertThat(deletedAt(fixture.question().getId())).isNull();
    }

    @Test
    void 이미_삭제된_질문의_재삭제_요청은_404를_반환한다() throws Exception {
        DeleteFixture fixture = createFixture(true);

        deleteQuestion(fixture.question().getId(), fixture.creatorLogin())
            .andExpect(status().isNoContent());
        deleteQuestion(fixture.question().getId(), fixture.creatorLogin())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"));
    }

    @Test
    void 종료된_세션의_질문은_삭제할_수_없다() throws Exception {
        DeleteFixture fixture = createFixture(true);
        fixture.presentationSession().end(Instant.parse("2026-06-17T11:00:00Z"));
        sessionRepository.saveAndFlush(fixture.presentationSession());

        deleteQuestion(fixture.question().getId(), fixture.creatorLogin())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SESSION_ENDED"));

        assertThat(deletedAt(fixture.question().getId())).isNull();
    }

    @Test
    void 세션에_입장하지_않은_회원은_질문을_삭제할_수_없다() throws Exception {
        DeleteFixture fixture = createFixture(true);

        deleteQuestion(fixture.question().getId(), fixture.outsiderLogin())
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("SESSION_PARTICIPANT_REQUIRED"));

        assertThat(deletedAt(fixture.question().getId())).isNull();
    }

    @Test
    void 인증되지_않은_요청은_401을_반환한다() throws Exception {
        DeleteFixture fixture = createFixture(true);

        mockMvc.perform(delete("/questions/{questionId}", fixture.question().getId())
                .with(csrf()))
            .andExpect(status().isUnauthorized());

        assertThat(deletedAt(fixture.question().getId())).isNull();
    }

    private DeleteFixture createFixture(boolean questionerIsCreator) throws Exception {
        User creator = saveUser("세션 생성자");
        User questioner = questionerIsCreator ? creator : saveUser("질문 작성자");
        User member = saveUser("다른 참여자");
        User outsider = saveUser("미입장 회원");
        Organization organization = organizationRepository.save(Organization.builder()
            .name("question-delete-" + UUID.randomUUID().toString().substring(0, 8))
            .detail("질문 삭제 통합 테스트 그룹")
            .build());

        List<UserGroup> memberships = new java.util.ArrayList<>();
        memberships.add(membership(creator, organization, UserGroupRole.ADMIN));
        if (!questionerIsCreator) {
            memberships.add(membership(questioner, organization, UserGroupRole.USER));
        }
        memberships.add(membership(member, organization, UserGroupRole.USER));
        memberships.add(membership(outsider, organization, UserGroupRole.USER));
        userGroupRepository.saveAll(memberships);

        Session presentationSession = sessionRepository.save(Session.builder()
            .organizationId(organization.getId())
            .creatorId(creator.getId())
            .title("질문 삭제 테스트 세션")
            .build());
        Participant creatorParticipant = saveParticipant(creator, presentationSession);
        Participant questionerParticipant = questionerIsCreator
            ? creatorParticipant
            : saveParticipant(questioner, presentationSession);
        Participant memberParticipant = saveParticipant(member, presentationSession);

        Presentation presentation = Presentation.builder()
            .sessionId(presentationSession.getId())
            .presenterId(creator.getId())
            .title("질문 삭제 테스트 발표")
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
            .questionerId(questionerParticipant.getId())
            .content("삭제할 질문입니다")
            .anonymous(false)
            .pageStart(1)
            .pageEnd(1)
            .build());

        MockHttpSession creatorLogin = login(creator.getEmail());
        MockHttpSession questionerLogin = questionerIsCreator
            ? creatorLogin
            : login(questioner.getEmail());
        MockHttpSession memberLogin = login(member.getEmail());
        MockHttpSession outsiderLogin = login(outsider.getEmail());

        return new DeleteFixture(
            creator,
            questioner,
            member,
            outsider,
            presentationSession,
            creatorParticipant,
            questionerParticipant,
            memberParticipant,
            presentation,
            question,
            creatorLogin,
            questionerLogin,
            memberLogin,
            outsiderLogin
        );
    }

    private DeleteFixture createGuestQuestionFixture() throws Exception {
        User creator = saveUser("게스트 질문 세션 생성자");
        User member = saveUser("게스트 질문 다른 참여자");
        User outsider = saveUser("게스트 질문 미입장 회원");
        Organization organization = organizationRepository.save(Organization.builder()
            .name("question-delete-guest-" + UUID.randomUUID().toString().substring(0, 8))
            .detail("게스트 질문 삭제 통합 테스트 그룹")
            .build());

        userGroupRepository.saveAll(List.of(
            membership(creator, organization, UserGroupRole.ADMIN),
            membership(member, organization, UserGroupRole.USER),
            membership(outsider, organization, UserGroupRole.USER)
        ));

        Session presentationSession = sessionRepository.save(Session.builder()
            .organizationId(organization.getId())
            .creatorId(creator.getId())
            .title("게스트 질문 삭제 테스트 세션")
            .build());
        Participant creatorParticipant = saveParticipant(creator, presentationSession);
        Participant memberParticipant = saveParticipant(member, presentationSession);
        Participant guestQuestioner = participantRepository.save(
            Participant.guest("질문 작성 게스트", presentationSession)
        );

        Presentation presentation = Presentation.builder()
            .sessionId(presentationSession.getId())
            .presenterId(creator.getId())
            .title("게스트 질문 삭제 테스트 발표")
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
            .questionerId(guestQuestioner.getId())
            .content("게스트가 작성한 질문입니다")
            .anonymous(false)
            .pageStart(1)
            .pageEnd(1)
            .build());

        return new DeleteFixture(
            creator,
            null,
            member,
            outsider,
            presentationSession,
            creatorParticipant,
            guestQuestioner,
            memberParticipant,
            presentation,
            question,
            login(creator.getEmail()),
            null,
            login(member.getEmail()),
            login(outsider.getEmail())
        );
    }

    private UserGroup membership(User user, Organization organization, UserGroupRole role) {
        return UserGroup.builder()
            .userId(user.getId())
            .organization(organization)
            .role(role)
            .build();
    }

    private User saveUser(String nickname) {
        return userRepository.save(User.builder()
            .email("question-delete-" + UUID.randomUUID() + "@example.com")
            .nickname(nickname + "-" + UUID.randomUUID().toString().substring(0, 8))
            .password(passwordEncoder.encode(PASSWORD))
            .build());
    }

    private Participant saveParticipant(User user, Session presentationSession) {
        return participantRepository.save(Participant.member(user.getId(), presentationSession));
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

    private ResultActions deleteQuestion(UUID questionId, MockHttpSession loginSession) throws Exception {
        return mockMvc.perform(delete("/questions/{questionId}", questionId)
            .with(csrf())
            .session(loginSession));
    }

    private ResultActions deleteGuestQuestion(
        UUID questionId,
        Participant guest,
        Session presentationSession
    ) throws Exception {
        var guestAuthentication = UsernamePasswordAuthenticationToken.authenticated(
            new GuestPrincipal(guest.getId(), presentationSession.getId()),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_GUEST"))
        );

        return mockMvc.perform(delete("/questions/{questionId}", questionId)
            .with(csrf())
            .with(authentication(guestAuthentication)));
    }

    private ResultActions getQuestions(UUID presentationId, MockHttpSession loginSession) throws Exception {
        return mockMvc.perform(get("/presentations/{presentationId}/questions", presentationId)
            .session(loginSession));
    }

    private Object deletedAt(UUID questionId) {
        return jdbcTemplate.queryForObject(
            "SELECT deleted_at FROM questions WHERE id = ?",
            (resultSet, rowNum) -> resultSet.getObject("deleted_at"),
            questionId
        );
    }

    private record DeleteFixture(
        User creator,
        User questioner,
        User member,
        User outsider,
        Session presentationSession,
        Participant creatorParticipant,
        Participant questionerParticipant,
        Participant memberParticipant,
        Presentation presentation,
        Question question,
        MockHttpSession creatorLogin,
        MockHttpSession questionerLogin,
        MockHttpSession memberLogin,
        MockHttpSession outsiderLogin
    ) {
    }
}
