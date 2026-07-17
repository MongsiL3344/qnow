package io.github.mongsil3344.qnow.bff.infrastructure.web;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.mongsil3344.qnow.organization.domain.Organization;
import io.github.mongsil3344.qnow.organization.domain.UserGroup;
import io.github.mongsil3344.qnow.organization.domain.UserGroupRole;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.OrganizationRepository;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.UserGroupRepository;
import io.github.mongsil3344.qnow.session.api.GuestPrincipal;
import io.github.mongsil3344.qnow.session.domain.Participant;
import io.github.mongsil3344.qnow.session.domain.Session;
import io.github.mongsil3344.qnow.session.infrastructure.repo.ParticipantRepository;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionRepository;
import io.github.mongsil3344.qnow.user.domain.User;
import io.github.mongsil3344.qnow.user.infrastructure.repo.UserRepository;
import java.sql.Timestamp;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class SessionScreenControllerTest {

    private static final String PASSWORD = "password123";
    private static final Instant START_AT = Instant.parse("2026-07-17T10:00:00Z");
    private static final Instant END_AT = Instant.parse("2026-07-17T11:00:00Z");

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 관리자와_일반_회원은_세션_화면과_역할별_권한을_조회한다() throws Exception {
        SessionFixture fixture = createActiveFixture();

        mockMvc.perform(get("/organizations/{organizationId}/sessions/{sessionId}",
                fixture.organization().getId(),
                fixture.session().getId())
                .session(login(fixture.creator().getEmail())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(fixture.session().getId().toString()))
            .andExpect(jsonPath("$.title").value(fixture.session().getTitle()))
            .andExpect(jsonPath("$.creatorName").value(fixture.creator().getNickname()))
            .andExpect(jsonPath("$.startAt").value("2026-07-17T10:00:00Z"))
            .andExpect(jsonPath("$.endAt").value(nullValue()))
            .andExpect(jsonPath("$.participantCount").value(2))
            .andExpect(jsonPath("$.canUpload").value(true))
            .andExpect(jsonPath("$.canEnd").value(true));

        mockMvc.perform(get("/organizations/{organizationId}/sessions/{sessionId}",
                fixture.organization().getId(),
                fixture.session().getId())
                .session(login(fixture.member().getEmail())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.participantCount").value(2))
            .andExpect(jsonPath("$.canUpload").value(true))
            .andExpect(jsonPath("$.canEnd").value(false));
    }

    @Test
    void 활성_게스트는_권한_없이_세션_화면을_조회한다() throws Exception {
        SessionFixture fixture = createActiveFixture();

        mockMvc.perform(get("/organizations/{organizationId}/sessions/{sessionId}",
                fixture.organization().getId(),
                fixture.session().getId())
                .session(guestSession(fixture.guest().getId(), fixture.session().getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(fixture.session().getId().toString()))
            .andExpect(jsonPath("$.title").value(fixture.session().getTitle()))
            .andExpect(jsonPath("$.creatorName").value(fixture.creator().getNickname()))
            .andExpect(jsonPath("$.startAt").value("2026-07-17T10:00:00Z"))
            .andExpect(jsonPath("$.endAt").value(nullValue()))
            .andExpect(jsonPath("$.participantCount").value(2))
            .andExpect(jsonPath("$.canUpload").value(false))
            .andExpect(jsonPath("$.canEnd").value(false));
    }

    @Test
    void 회원은_종료된_세션을_권한_없이_조회하고_종료_당시_참여자_수를_확인한다() throws Exception {
        SessionFixture fixture = createActiveFixture();
        endSession(fixture);

        mockMvc.perform(get("/organizations/{organizationId}/sessions/{sessionId}",
                fixture.organization().getId(),
                fixture.session().getId())
                .session(login(fixture.member().getEmail())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.endAt").value("2026-07-17T11:00:00Z"))
            .andExpect(jsonPath("$.participantCount").value(2))
            .andExpect(jsonPath("$.canUpload").value(false))
            .andExpect(jsonPath("$.canEnd").value(false));
    }

    @Test
    void 삭제된_진행자의_이름은_알_수_없는_사용자로_표시한다() throws Exception {
        SessionFixture fixture = createActiveFixture();
        MockHttpSession memberSession = login(fixture.member().getEmail());
        jdbcTemplate.update(
            "update users set deleted_at = ? where id = ?",
            Timestamp.from(Instant.parse("2026-07-18T00:00:00Z")),
            fixture.creator().getId()
        );

        mockMvc.perform(get("/organizations/{organizationId}/sessions/{sessionId}",
                fixture.organization().getId(),
                fixture.session().getId())
                .session(memberSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.creatorName").value("알 수 없는 사용자"));
    }

    @Test
    void 세션_화면_조회는_인증이_필요하다() throws Exception {
        mockMvc.perform(get("/organizations/{organizationId}/sessions/{sessionId}",
                UUID.randomUUID(),
                UUID.randomUUID()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 조직_외부_회원은_세션_화면을_조회할_수_없다() throws Exception {
        SessionFixture fixture = createActiveFixture();
        User outsider = saveUser("외부인");

        mockMvc.perform(get("/organizations/{organizationId}/sessions/{sessionId}",
                fixture.organization().getId(),
                fixture.session().getId())
                .session(login(outsider.getEmail())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ORGANIZATION_MEMBER_REQUIRED"));
    }

    @Test
    void 잘못된_조직과_세션_조합은_존재하지_않는_세션으로_응답한다() throws Exception {
        SessionFixture fixture = createActiveFixture();
        Organization otherOrganization = saveOrganization();

        mockMvc.perform(get("/organizations/{organizationId}/sessions/{sessionId}",
                otherOrganization.getId(),
                fixture.session().getId())
                .session(login(fixture.member().getEmail())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void 게스트는_다른_세션이나_퇴장한_세션이나_종료된_세션을_조회할_수_없다() throws Exception {
        SessionFixture first = createActiveFixture();
        SessionFixture second = createActiveFixture();
        MockHttpSession firstGuestSession = guestSession(first.guest().getId(), first.session().getId());

        assertGuestParticipantRequired(
            second.organization().getId(),
            second.session().getId(),
            firstGuestSession
        );

        first.guest().exit(END_AT);
        participantRepository.saveAndFlush(first.guest());
        assertGuestParticipantRequired(
            first.organization().getId(),
            first.session().getId(),
            firstGuestSession
        );

        SessionFixture ended = createActiveFixture();
        ended.session().end(END_AT);
        sessionRepository.saveAndFlush(ended.session());
        assertGuestParticipantRequired(
            ended.organization().getId(),
            ended.session().getId(),
            guestSession(ended.guest().getId(), ended.session().getId())
        );
    }

    private void assertGuestParticipantRequired(
        UUID organizationId,
        UUID sessionId,
        MockHttpSession guestSession
    ) throws Exception {
        mockMvc.perform(get("/organizations/{organizationId}/sessions/{sessionId}", organizationId, sessionId)
                .session(guestSession))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("SESSION_PARTICIPANT_REQUIRED"));
    }

    private SessionFixture createActiveFixture() {
        User creator = saveUser("진행자");
        User member = saveUser("일반회원");
        Organization organization = saveOrganization();
        userGroupRepository.save(UserGroup.builder()
            .userId(creator.getId())
            .organization(organization)
            .role(UserGroupRole.ADMIN)
            .build());
        userGroupRepository.save(UserGroup.builder()
            .userId(member.getId())
            .organization(organization)
            .role(UserGroupRole.USER)
            .build());

        Session session = sessionRepository.save(Session.builder()
            .organizationId(organization.getId())
            .creatorId(creator.getId())
            .title("세션 화면-" + UUID.randomUUID())
            .startAt(START_AT)
            .build());
        Participant creatorParticipant = participantRepository.save(Participant.member(creator.getId(), session));
        Participant guest = participantRepository.save(Participant.guest("발표 손님", session));

        return new SessionFixture(
            creator,
            member,
            organization,
            session,
            creatorParticipant,
            guest
        );
    }

    private void endSession(SessionFixture fixture) {
        fixture.session().end(END_AT);
        fixture.creatorParticipant().exit(END_AT);
        fixture.guest().exit(END_AT);
        sessionRepository.save(fixture.session());
        participantRepository.saveAll(List.of(fixture.creatorParticipant(), fixture.guest()));
        participantRepository.flush();
    }

    private User saveUser(String nicknamePrefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(User.builder()
            .email(nicknamePrefix + "-" + suffix + "@example.com")
            .nickname(nicknamePrefix + "-" + suffix)
            .password(passwordEncoder.encode(PASSWORD))
            .build());
    }

    private Organization saveOrganization() {
        return organizationRepository.save(Organization.builder()
            .name("session-screen-" + UUID.randomUUID().toString().substring(0, 8))
            .detail("세션 화면 조회 통합 테스트 조직입니다.")
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

    private MockHttpSession guestSession(UUID participantId, UUID sessionId) {
        GuestPrincipal principal = new GuestPrincipal(participantId, sessionId);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_GUEST"))
        ));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            context
        );
        return session;
    }

    private record SessionFixture(
        User creator,
        User member,
        Organization organization,
        Session session,
        Participant creatorParticipant,
        Participant guest
    ) {
    }
}
