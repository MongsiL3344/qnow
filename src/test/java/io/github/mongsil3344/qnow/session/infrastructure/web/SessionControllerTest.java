package io.github.mongsil3344.qnow.session.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import io.github.mongsil3344.qnow.organization.domain.Organization;
import io.github.mongsil3344.qnow.organization.domain.UserGroup;
import io.github.mongsil3344.qnow.organization.domain.UserGroupRole;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.OrganizationRepository;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.UserGroupRepository;
import io.github.mongsil3344.qnow.session.domain.Participant;
import io.github.mongsil3344.qnow.session.domain.Session;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class SessionControllerTest {

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
    private SessionParticipateCodeRepository participateCodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void 세션_생성은_요청_본문의_시작_시간을_사용한다() throws Exception {
        String password = "password123";
        User creator = saveUser("session-creator-" + UUID.randomUUID() + "@example.com", password);
        MockHttpSession loginSession = login(creator.getEmail(), password);

        Organization organization = organizationRepository.save(Organization.builder()
            .name("org-" + UUID.randomUUID().toString().substring(0, 8))
            .detail("세션 생성 테스트 그룹입니다.")
            .build());

        userGroupRepository.save(UserGroup.builder()
            .userId(creator.getId())
            .organization(organization)
            .role(UserGroupRole.ADMIN)
            .build());

        String title = "session-" + UUID.randomUUID();
        Instant startAt = Instant.parse("2026-06-17T10:00:00Z");

        mockMvc.perform(post("/organizations/{organizationId}/sessions", organization.getId())
                .with(csrf())
                .session(loginSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "%s",
                      "startAt": "%s"
                    }
                    """.formatted(title, startAt)))
            .andExpect(status().isCreated());

        Session createdSession = sessionRepository
            .findAllByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(organization.getId())
            .stream()
            .filter(session -> title.equals(session.getTitle()))
            .findFirst()
            .orElseThrow();

        assertThat(createdSession.getCreatorId()).isEqualTo(creator.getId());
        assertThat(createdSession.getStartAt()).isEqualTo(startAt);
        assertThat(participantRepository.existsByUserIdAndSessionIdAndDeletedAtIsNull(
            creator.getId(),
            createdSession.getId()
        )).isTrue();
        assertThat(participateCodeRepository.findActiveBySessionId(createdSession.getId()))
            .isPresent();
    }

    @Test
    void 세션에서_나가면_활성_참여자를_논리_삭제한다() throws Exception {
        String password = "password123";
        User user = saveUser("session-exit-" + UUID.randomUUID() + "@example.com", password);
        MockHttpSession loginSession = login(user.getEmail(), password);

        Organization organization = organizationRepository.save(Organization.builder()
            .name("org-" + UUID.randomUUID().toString().substring(0, 8))
            .detail("세션 퇴장 테스트 그룹입니다.")
            .build());

        userGroupRepository.save(UserGroup.builder()
            .userId(user.getId())
            .organization(organization)
            .role(UserGroupRole.USER)
            .build());

        Session session = sessionRepository.save(Session.builder()
            .organizationId(organization.getId())
            .creatorId(user.getId())
            .title("exit-session-" + UUID.randomUUID())
            .startAt(Instant.parse("2026-06-17T10:00:00Z"))
            .build());

        Participant participant = participantRepository.save(Participant.member(user.getId(), session));

        mockMvc.perform(post("/organizations/{organizationId}/sessions/{sessionId}/participants/exit",
                organization.getId(),
                session.getId())
                .with(csrf())
                .session(loginSession))
            .andExpect(status().isNoContent());

        Participant exitedParticipant = participantRepository.findById(participant.getId()).orElseThrow();

        assertThat(exitedParticipant.getDeletedAt()).isNotNull();
        assertThat(participantRepository.existsByUserIdAndSessionIdAndDeletedAtIsNull(
            user.getId(),
            session.getId()
        )).isFalse();
    }

    @Test
    void 세션을_종료하면_활성_참여자를_논리_삭제하고_멱등성을_보장한다() throws Exception {
        String password = "password123";
        User admin = saveUser("session-end-admin-" + UUID.randomUUID() + "@example.com", password);
        User member = saveUser("session-end-member-" + UUID.randomUUID() + "@example.com", password);
        User exitedUser = saveUser("session-end-exited-" + UUID.randomUUID() + "@example.com", password);
        MockHttpSession loginSession = login(admin.getEmail(), password);

        Organization organization = organizationRepository.save(Organization.builder()
            .name("org-" + UUID.randomUUID().toString().substring(0, 8))
            .detail("세션 종료 테스트 그룹입니다.")
            .build());

        userGroupRepository.save(UserGroup.builder()
            .userId(admin.getId())
            .organization(organization)
            .role(UserGroupRole.ADMIN)
            .build());

        Session session = sessionRepository.save(Session.builder()
            .organizationId(organization.getId())
            .creatorId(admin.getId())
            .title("end-session-" + UUID.randomUUID())
            .startAt(Instant.parse("2026-06-17T10:00:00Z"))
            .build());

        Participant adminParticipant = participantRepository.save(Participant.member(admin.getId(), session));
        Participant memberParticipant = participantRepository.save(Participant.member(member.getId(), session));
        Participant exitedParticipant = participantRepository.save(Participant.member(exitedUser.getId(), session));
        Instant previousExitAt = Instant.parse("2026-06-17T10:30:00Z");
        exitedParticipant.exit(previousExitAt);
        participantRepository.saveAndFlush(exitedParticipant);

        mockMvc.perform(post("/organizations/{organizationId}/sessions/{sessionId}/end",
                organization.getId(),
                session.getId())
                .with(csrf())
                .session(loginSession))
            .andExpect(status().isNoContent());

        Session endedSession = sessionRepository.findById(session.getId()).orElseThrow();
        Participant endedAdmin = participantRepository.findById(adminParticipant.getId()).orElseThrow();
        Participant endedMember = participantRepository.findById(memberParticipant.getId()).orElseThrow();
        Participant previouslyExited = participantRepository.findById(exitedParticipant.getId()).orElseThrow();

        assertThat(endedSession.getEndAt()).isNotNull();
        assertThat(endedAdmin.getDeletedAt()).isEqualTo(endedSession.getEndAt());
        assertThat(endedMember.getDeletedAt()).isEqualTo(endedSession.getEndAt());
        assertThat(previouslyExited.getDeletedAt()).isEqualTo(previousExitAt);

        Instant originalEndAt = endedSession.getEndAt();

        mockMvc.perform(post("/organizations/{organizationId}/sessions/{sessionId}/end",
                organization.getId(),
                session.getId())
                .with(csrf())
                .session(loginSession))
            .andExpect(status().isNoContent());

        assertThat(sessionRepository.findById(session.getId()).orElseThrow().getEndAt())
            .isEqualTo(originalEndAt);
        assertThat(participantRepository.findById(adminParticipant.getId()).orElseThrow().getDeletedAt())
            .isEqualTo(originalEndAt);
    }

    @Test
    void 관리자가_아닌_사용자의_세션_종료를_거부한다() throws Exception {
        String password = "password123";
        User user = saveUser("session-end-user-" + UUID.randomUUID() + "@example.com", password);
        MockHttpSession loginSession = login(user.getEmail(), password);

        Organization organization = organizationRepository.save(Organization.builder()
            .name("org-" + UUID.randomUUID().toString().substring(0, 8))
            .detail("세션 종료 권한 테스트 그룹입니다.")
            .build());

        userGroupRepository.save(UserGroup.builder()
            .userId(user.getId())
            .organization(organization)
            .role(UserGroupRole.USER)
            .build());

        Session session = sessionRepository.save(Session.builder()
            .organizationId(organization.getId())
            .creatorId(user.getId())
            .title("forbidden-end-session-" + UUID.randomUUID())
            .build());

        mockMvc.perform(post("/organizations/{organizationId}/sessions/{sessionId}/end",
                organization.getId(),
                session.getId())
                .with(csrf())
                .session(loginSession))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ORGANIZATION_ADMIN_REQUIRED"));

        assertThat(sessionRepository.findById(session.getId()).orElseThrow().getEndAt()).isNull();
    }

    @Test
    void 종료된_세션_참여는_세션_종료_충돌_오류를_반환한다() throws Exception {
        String password = "password123";
        User user = saveUser("session-ended-join-" + UUID.randomUUID() + "@example.com", password);
        MockHttpSession loginSession = login(user.getEmail(), password);

        Organization organization = organizationRepository.save(Organization.builder()
            .name("org-" + UUID.randomUUID().toString().substring(0, 8))
            .detail("종료 세션 입장 테스트 그룹입니다.")
            .build());

        userGroupRepository.save(UserGroup.builder()
            .userId(user.getId())
            .organization(organization)
            .role(UserGroupRole.USER)
            .build());

        Session session = sessionRepository.save(Session.builder()
            .organizationId(organization.getId())
            .creatorId(user.getId())
            .title("ended-session-" + UUID.randomUUID())
            .endAt(Instant.parse("2026-06-17T11:00:00Z"))
            .build());

        mockMvc.perform(post("/organizations/{organizationId}/sessions/{sessionId}/participants",
                organization.getId(),
                session.getId())
                .with(csrf())
                .session(loginSession))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SESSION_ENDED"));

        assertThat(participantRepository.existsByUserIdAndSessionIdAndDeletedAtIsNull(
            user.getId(),
            session.getId()
        )).isFalse();
    }

    private User saveUser(String email, String rawPassword) {
        User user = User.builder()
            .email(email)
            .nickname("tester")
            .password(passwordEncoder.encode(rawPassword))
            .build();

        return userRepository.save(user);
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(email, password)))
            .andExpect(status().isOk())
            .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String loginBody(String email, String password) {
        return """
            {
              "email": "%s",
              "password": "%s"
            }
            """.formatted(email, password);
    }
}
