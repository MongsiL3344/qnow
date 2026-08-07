package io.github.mongsil3344.qnow.question.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import io.github.mongsil3344.qnow.question.domain.QuestionKind;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ControlRequestControllerTest {

    private static final String PASSWORD = "password123";
    private static final int PAGE_COUNT = 12;

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

    @Test
    void 제어_요청을_생성하면_질문_목록에_노출된다() throws Exception {
        ControlRequestFixture fixture = createFixture();

        postControlRequest(fixture, 7)
            .andExpect(status().isCreated())
            .andExpect(content().string(""));

        Question controlRequest = findQuestions(fixture.presentation().getId()).stream()
            .findFirst()
            .orElseThrow();
        assertThat(controlRequest.getKind()).isEqualTo(QuestionKind.CONTROL_REQUEST);
        assertThat(controlRequest.getContent()).isEmpty();
        assertThat(controlRequest.isAnonymous()).isFalse();
        assertThat(controlRequest.getPageStart()).isEqualTo(7);
        assertThat(controlRequest.getPageEnd()).isEqualTo(7);
        assertThat(controlRequest.getSelection()).isNull();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/presentations/{presentationId}/questions", fixture.presentation().getId())
                .session(fixture.requesterLogin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].kind").value("CONTROL_REQUEST"))
            .andExpect(jsonPath("$.content[0].approved").value(false))
            .andExpect(jsonPath("$.content[0].questionerParticipantId")
                .value(fixture.requesterParticipant().getId().toString()));
    }

    @Test
    void 게스트가_제어_요청을_생성할_수_있다() throws Exception {
        ControlRequestFixture fixture = createFixture();
        Participant guest = participantRepository.saveAndFlush(
            Participant.guest("제어 요청 게스트", fixture.session())
        );

        postGuestControlRequest(fixture, guest, 7)
            .andExpect(status().isCreated())
            .andExpect(content().string(""));

        Question controlRequest = findQuestions(fixture.presentation().getId()).stream()
            .findFirst()
            .orElseThrow();
        assertThat(controlRequest.getKind()).isEqualTo(QuestionKind.CONTROL_REQUEST);
        assertThat(controlRequest.getQuestionerId()).isEqualTo(guest.getId());
        assertThat(controlRequest.getPageStart()).isEqualTo(7);
        assertThat(controlRequest.getPageEnd()).isEqualTo(7);
    }

    @Test
    void 종료된_세션에는_제어_요청을_생성할_수_없다() throws Exception {
        ControlRequestFixture fixture = createFixture();
        fixture.session().end(Instant.parse("2026-06-17T11:00:00Z"));
        sessionRepository.saveAndFlush(fixture.session());

        postControlRequest(fixture, 7)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SESSION_ENDED"));

        assertThat(findQuestions(fixture.presentation().getId())).isEmpty();
    }

    @Test
    void 개설자가_제어_요청을_승인한다() throws Exception {
        ControlRequestFixture fixture = createFixture();
        postControlRequest(fixture, 7).andExpect(status().isCreated());
        Question controlRequest = findQuestions(fixture.presentation().getId()).stream()
            .findFirst()
            .orElseThrow();

        putApproval(controlRequest.getId(), fixture.creatorLogin())
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));
        Instant approvedAt = questionRepository.findById(controlRequest.getId())
            .orElseThrow()
            .getApprovedAt();

        assertThat(approvedAt).isNotNull();

        putApproval(controlRequest.getId(), fixture.creatorLogin())
            .andExpect(status().isNoContent());

        assertThat(questionRepository.findById(controlRequest.getId())
            .orElseThrow()
            .getApprovedAt()).isEqualTo(approvedAt);
    }

    @Test
    void 개설자가_아니면_승인할_수_없다() throws Exception {
        ControlRequestFixture fixture = createFixture();
        postControlRequest(fixture, 7).andExpect(status().isCreated());
        Question controlRequest = findQuestions(fixture.presentation().getId()).stream()
            .findFirst()
            .orElseThrow();

        putApproval(controlRequest.getId(), fixture.requesterLogin())
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("CONTROL_REQUEST_APPROVAL_FORBIDDEN"));

        assertThat(questionRepository.findById(controlRequest.getId())
            .orElseThrow()
            .getApprovedAt()).isNull();
    }

    @Test
    void 일반_질문은_승인할_수_없다() throws Exception {
        ControlRequestFixture fixture = createFixture();
        Question question = questionRepository.saveAndFlush(Question.builder()
            .presentationId(fixture.presentation().getId())
            .questionerId(fixture.requesterParticipant().getId())
            .content("일반 질문입니다")
            .anonymous(false)
            .pageStart(1)
            .pageEnd(1)
            .build());

        putApproval(question.getId(), fixture.creatorLogin())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("NOT_CONTROL_REQUEST"));
    }

    private ControlRequestFixture createFixture() throws Exception {
        User creator = saveUser("세션 개설자");
        User requester = saveUser("제어 요청자");
        Organization organization = organizationRepository.save(Organization.builder()
            .name("control-request-" + UUID.randomUUID().toString().substring(0, 8))
            .detail("발표 제어 요청 통합 테스트 그룹")
            .build());

        userGroupRepository.saveAll(List.of(
            membership(creator, organization, UserGroupRole.ADMIN),
            membership(requester, organization, UserGroupRole.USER)
        ));

        Session session = sessionRepository.save(Session.builder()
            .organizationId(organization.getId())
            .creatorId(creator.getId())
            .title("발표 제어 요청 테스트 세션")
            .build());
        Participant creatorParticipant = saveParticipant(creator, session);
        Participant requesterParticipant = saveParticipant(requester, session);
        Presentation presentation = Presentation.builder()
            .sessionId(session.getId())
            .presenterId(creator.getId())
            .title("발표 제어 요청 테스트 발표")
            .pageCount(PAGE_COUNT)
            .build();
        presentation.assignS3Key("presentations/%s/%s/original.pdf".formatted(
            session.getId(),
            presentation.getId()
        ));
        presentation.setStatusUploaded();
        presentation = presentationRepository.save(presentation);

        return new ControlRequestFixture(
            creator,
            requester,
            session,
            creatorParticipant,
            requesterParticipant,
            presentation,
            login(creator.getEmail()),
            login(requester.getEmail())
        );
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

    private User saveUser(String nickname) {
        return userRepository.save(User.builder()
            .email("control-request-" + UUID.randomUUID() + "@example.com")
            .nickname(nickname + "-" + UUID.randomUUID().toString().substring(0, 8))
            .password(passwordEncoder.encode(PASSWORD))
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

    private ResultActions postControlRequest(ControlRequestFixture fixture, int pageNumber) throws Exception {
        return mockMvc.perform(post(
                "/presentations/{presentationId}/questions/control-requests",
                fixture.presentation().getId()
            )
            .with(csrf())
            .session(fixture.requesterLogin())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"pageNumber\": %d}".formatted(pageNumber)));
    }

    private ResultActions postGuestControlRequest(
        ControlRequestFixture fixture,
        Participant guest,
        int pageNumber
    ) throws Exception {
        var guestAuthentication = UsernamePasswordAuthenticationToken.authenticated(
            new GuestPrincipal(guest.getId(), fixture.session().getId()),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_GUEST"))
        );

        return mockMvc.perform(post(
                "/presentations/{presentationId}/questions/control-requests",
                fixture.presentation().getId()
            )
            .with(csrf())
            .with(authentication(guestAuthentication))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"pageNumber\": %d}".formatted(pageNumber)));
    }

    private ResultActions putApproval(UUID questionId, MockHttpSession loginSession) throws Exception {
        return mockMvc.perform(put("/questions/{questionId}/approval", questionId)
            .with(csrf())
            .session(loginSession));
    }

    private List<Question> findQuestions(UUID presentationId) {
        return questionRepository.findAll().stream()
            .filter(question -> presentationId.equals(question.getPresentationId()))
            .toList();
    }

    private record ControlRequestFixture(
        User creator,
        User requester,
        Session session,
        Participant creatorParticipant,
        Participant requesterParticipant,
        Presentation presentation,
        MockHttpSession creatorLogin,
        MockHttpSession requesterLogin
    ) {
    }
}
