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
import io.github.mongsil3344.qnow.session.domain.Participant;
import io.github.mongsil3344.qnow.session.domain.Session;
import io.github.mongsil3344.qnow.session.infrastructure.repo.ParticipantRepository;
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
class OrganizationDetailControllerTest {

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

    @Test
    void 조직_상세_조회는_조직과_세션을_반환한다() throws Exception {
        String password = "password123";
        User creator = saveUser("creator-" + UUID.randomUUID() + "@example.com", "김민준", password);
        User audience = saveUser("audience-" + UUID.randomUUID() + "@example.com", "이서연", password);
        MockHttpSession loginSession = login(creator.getEmail(), password);

        Organization organization = organizationRepository.save(Organization.builder()
            .name("org-" + UUID.randomUUID().toString().substring(0, 8))
            .detail("백엔드 발표 자료를 모아 질문하고 복습하는 그룹입니다.")
            .build());

        userGroupRepository.save(UserGroup.builder()
            .userId(creator.getId())
            .organization(organization)
            .role(UserGroupRole.ADMIN)
            .build());
        userGroupRepository.save(UserGroup.builder()
            .userId(audience.getId())
            .organization(organization)
            .role(UserGroupRole.USER)
            .build());

        mockMvc.perform(get("/organizations/{organizationId}", organization.getId())
                .session(loginSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isAdmin").value(true))
            .andExpect(jsonPath("$.sessions").isEmpty());

        Session studySession = sessionRepository.save(Session.builder()
            .organizationId(organization.getId())
            .creatorId(creator.getId())
            .title("Spring 트랜잭션 전파")
            .startAt(Instant.parse("2026-06-17T10:00:00Z"))
            .build());

        participantRepository.save(Participant.builder()
            .userId(creator.getId())
            .session(studySession)
            .build());
        participantRepository.save(Participant.builder()
            .userId(audience.getId())
            .session(studySession)
            .build());

        mockMvc.perform(get("/organizations/{organizationId}", organization.getId())
                .session(loginSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(organization.getId().toString()))
            .andExpect(jsonPath("$.name").value(organization.getName()))
            .andExpect(jsonPath("$.detail").value(organization.getDetail()))
            .andExpect(jsonPath("$.memberCount").value(2))
            .andExpect(jsonPath("$.isAdmin").value(true))
            .andExpect(jsonPath("$.sessions[0].id").value(studySession.getId().toString()))
            .andExpect(jsonPath("$.sessions[0].title").value("Spring 트랜잭션 전파"))
            .andExpect(jsonPath("$.sessions[0].creatorName").value("김민준"))
            .andExpect(jsonPath("$.sessions[0].startAt").value("2026-06-17T10:00:00Z"))
            .andExpect(jsonPath("$.sessions[0].endAt").value(nullValue()))
            .andExpect(jsonPath("$.sessions[0].participantCount").value(2))
            .andExpect(jsonPath("$.sessions[0].canEnd").value(true));

        MockHttpSession audienceLoginSession = login(audience.getEmail(), password);
        mockMvc.perform(get("/organizations/{organizationId}", organization.getId())
                .session(audienceLoginSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isAdmin").value(false))
            .andExpect(jsonPath("$.sessions[0].canEnd").value(false));

        studySession.end(Instant.parse("2026-06-17T11:00:00Z"));
        sessionRepository.saveAndFlush(studySession);

        mockMvc.perform(get("/organizations/{organizationId}", organization.getId())
                .session(loginSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isAdmin").value(true))
            .andExpect(jsonPath("$.sessions[0].endAt").value("2026-06-17T11:00:00Z"))
            .andExpect(jsonPath("$.sessions[0].canEnd").value(false));
    }

    private User saveUser(String email, String nickname, String rawPassword) {
        User user = User.builder()
            .email(email)
            .nickname(nickname)
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
