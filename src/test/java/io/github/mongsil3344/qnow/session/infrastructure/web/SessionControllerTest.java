package io.github.mongsil3344.qnow.session.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.mongsil3344.qnow.organization.domain.Organization;
import io.github.mongsil3344.qnow.organization.domain.UserGroup;
import io.github.mongsil3344.qnow.organization.domain.UserGroupRole;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.OrganizationRepository;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.UserGroupRepository;
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
    private PasswordEncoder passwordEncoder;

    @Test
    void createSessionUsesStartAtFromRequestBody() throws Exception {
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
