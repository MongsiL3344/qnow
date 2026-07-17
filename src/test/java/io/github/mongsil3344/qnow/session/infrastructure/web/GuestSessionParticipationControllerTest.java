package io.github.mongsil3344.qnow.session.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.mongsil3344.qnow.organization.domain.Organization;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.OrganizationRepository;
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import io.github.mongsil3344.qnow.question.domain.Question;
import io.github.mongsil3344.qnow.question.infrastructure.repo.QuestionRepository;
import io.github.mongsil3344.qnow.question.infrastructure.repo.QuestionUpvoteRepository;
import io.github.mongsil3344.qnow.session.api.GuestPrincipal;
import io.github.mongsil3344.qnow.session.domain.Participant;
import io.github.mongsil3344.qnow.session.domain.Session;
import io.github.mongsil3344.qnow.session.domain.SessionParticipateCode;
import io.github.mongsil3344.qnow.session.infrastructure.repo.ParticipantRepository;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionParticipateCodeRepository;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionRepository;
import io.github.mongsil3344.qnow.user.domain.User;
import io.github.mongsil3344.qnow.user.infrastructure.repo.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class GuestSessionParticipationControllerTest {

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
    private SessionParticipateCodeRepository participateCodeRepository;

    @Autowired
    private PresentationRepository presentationRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionUpvoteRepository questionUpvoteRepository;

    @Test
    void 비회원은_참가_후_공통_참여자_기능을_사용하고_퇴장할_수_있다() throws Exception {
        GuestFlowFixture fixture = createFixture(false);

        MvcResult joinResult = mockMvc.perform(post("/guest/session-participations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "%s",
                      "nickname": "  발표 손님  "
                    }
                    """.formatted(fixture.participateCode().getCode().replace("-", "").toLowerCase())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessionId").value(fixture.session().getId().toString()))
            .andExpect(jsonPath("$.organizationId").value(fixture.organization().getId().toString()))
            .andExpect(jsonPath("$.nickname").value("발표 손님"))
            .andReturn();

        MockHttpSession guestSession = (MockHttpSession) joinResult.getRequest().getSession(false);
        GuestPrincipal guestPrincipal = guestPrincipal(guestSession);
        Participant guest = participantRepository.findById(guestPrincipal.participantId()).orElseThrow();

        assertThat(guest.getUserId()).isNull();
        assertThat(guest.getGuestNickname()).isEqualTo("발표 손님");
        assertThat(guest.getSession().getId()).isEqualTo(fixture.session().getId());

        mockMvc.perform(get(
                "/organizations/{organizationId}/sessions/{sessionId}/presentations",
                fixture.organization().getId(),
                fixture.session().getId()
            ).session(guestSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.presentations[0].presentationId")
                .value(fixture.presentation().getId().toString()))
            .andExpect(jsonPath("$.presentations[0].canDelete").value(false));

        mockMvc.perform(post("/presentations/{presentationId}/questions", fixture.presentation().getId())
                .with(csrf())
                .session(guestSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "비회원 질문",
                      "anonymous": false,
                      "pageStart": 2,
                      "pageEnd": 2,
                      "selection": null
                    }
                    """))
            .andExpect(status().isCreated());

        Question guestQuestion = questionRepository.findAll().stream()
            .filter(question -> "비회원 질문".equals(question.getContent()))
            .findFirst()
            .orElseThrow();
        assertThat(guestQuestion.getQuestionerId()).isEqualTo(guest.getId());

        mockMvc.perform(get("/presentations/{presentationId}/questions", fixture.presentation().getId())
                .session(guestSession)
                .queryParam("sort", "latest")
                .queryParam("page", "0")
                .queryParam("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].content").value("비회원 질문"))
            .andExpect(jsonPath("$.content[0].questionerName").value("발표 손님"))
            .andExpect(jsonPath("$.content[0].mine").value(true));

        mockMvc.perform(put("/questions/{questionId}/upvote", guestQuestion.getId())
                .with(csrf())
                .session(guestSession))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("SELF_UPVOTE_NOT_ALLOWED"));

        mockMvc.perform(put("/questions/{questionId}/upvote", fixture.creatorQuestion().getId())
                .with(csrf())
                .session(guestSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.upvotedByMe").value(true))
            .andExpect(jsonPath("$.upvoteCount").value(1));

        assertThat(questionUpvoteRepository.findByQuestionIdAndVoterGuestParticipantId(
            fixture.creatorQuestion().getId(),
            guest.getId()
        )).isPresent();

        mockMvc.perform(get("/presentations/{presentationId}/questions", fixture.presentation().getId())
                .session(guestSession)
                .queryParam("sort", "most_upvoted")
                .queryParam("page", "0")
                .queryParam("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(fixture.creatorQuestion().getId().toString()))
            .andExpect(jsonPath("$.content[0].upvotedByMe").value(true));

        mockMvc.perform(delete("/questions/{questionId}/upvote", fixture.creatorQuestion().getId())
                .with(csrf())
                .session(guestSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.upvotedByMe").value(false))
            .andExpect(jsonPath("$.upvoteCount").value(0));

        mockMvc.perform(post("/logout")
                .with(csrf())
                .session(guestSession))
            .andExpect(status().isForbidden());

        mockMvc.perform(post(
                "/organizations/{organizationId}/sessions/{sessionId}/participants/exit",
                fixture.organization().getId(),
                fixture.session().getId()
            ).with(csrf()).session(guestSession))
            .andExpect(status().isNoContent());

        assertThat(participantRepository.findById(guest.getId()).orElseThrow().getDeletedAt()).isNotNull();
        assertThat(guestSession.isInvalid()).isTrue();
    }

    @Test
    void 존재하지_않는_참가_코드는_비회원_참여를_거부한다() throws Exception {
        mockMvc.perform(post("/guest/session-participations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "ABCD-EFGH",
                      "nickname": "손님"
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("SESSION_PARTICIPATE_CODE_NOT_FOUND"));
    }

    @Test
    void 종료된_세션의_참가_코드는_비회원_참여를_거부한다() throws Exception {
        GuestFlowFixture fixture = createFixture(true);

        mockMvc.perform(post("/guest/session-participations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "%s",
                      "nickname": "손님"
                    }
                    """.formatted(fixture.participateCode().getCode())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SESSION_ENDED"));
    }

    @Test
    void 비회원_참여_요청의_코드와_닉네임을_검증한다() throws Exception {
        mockMvc.perform(post("/guest/session-participations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "잘못된 코드",
                      "nickname": " "
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    private GuestPrincipal guestPrincipal(MockHttpSession session) {
        SecurityContext securityContext = (SecurityContext) session.getAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );
        assertThat(securityContext).isNotNull();
        assertThat(securityContext.getAuthentication().getPrincipal()).isInstanceOf(GuestPrincipal.class);
        return (GuestPrincipal) securityContext.getAuthentication().getPrincipal();
    }

    private GuestFlowFixture createFixture(boolean ended) {
        User creator = userRepository.save(User.builder()
            .email("guest-flow-" + UUID.randomUUID() + "@example.com")
            .nickname("발표자-" + UUID.randomUUID().toString().substring(0, 8))
            .password("encoded-password")
            .build());
        Organization organization = organizationRepository.save(Organization.builder()
            .name("guest-flow-" + UUID.randomUUID().toString().substring(0, 8))
            .detail("비회원 세션 참여 통합 테스트 조직입니다.")
            .build());
        Session session = sessionRepository.save(Session.builder()
            .organizationId(organization.getId())
            .creatorId(creator.getId())
            .title("guest-flow-session-" + UUID.randomUUID())
            .endAt(ended ? Instant.parse("2026-07-15T10:00:00Z") : null)
            .guestUpvoteAllowed(true)
            .build());
        Participant creatorParticipant = participantRepository.save(Participant.member(creator.getId(), session));
        SessionParticipateCode participateCode = participateCodeRepository.save(
            SessionParticipateCode.create(session)
        );
        Presentation presentation = Presentation.builder()
            .sessionId(session.getId())
            .presenterId(creator.getId())
            .title("비회원 참여 테스트 발표 자료")
            .pageCount(5)
            .build();
        presentation.assignS3Key("presentations/%s/%s/original.pdf".formatted(
            session.getId(),
            presentation.getId()
        ));
        presentation.setStatusUploaded();
        presentation = presentationRepository.save(presentation);
        Question creatorQuestion = questionRepository.save(Question.builder()
            .presentationId(presentation.getId())
            .questionerId(creatorParticipant.getId())
            .content("발표자의 질문")
            .anonymous(false)
            .pageStart(1)
            .pageEnd(1)
            .build());

        return new GuestFlowFixture(
            organization,
            session,
            participateCode,
            presentation,
            creatorQuestion
        );
    }

    private record GuestFlowFixture(
        Organization organization,
        Session session,
        SessionParticipateCode participateCode,
        Presentation presentation,
        Question creatorQuestion
    ) {
    }
}
