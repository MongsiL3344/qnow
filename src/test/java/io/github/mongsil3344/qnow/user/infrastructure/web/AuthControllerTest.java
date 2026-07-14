package io.github.mongsil3344.qnow.user.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.mongsil3344.qnow.organization.domain.Organization;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.OrganizationRepository;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.UserGroupRepository;
import io.github.mongsil3344.qnow.user.domain.User;
import io.github.mongsil3344.qnow.user.infrastructure.repo.UserRepository;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void 로그인하면_보안_컨텍스트를_세션에_저장한다() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@example.com";
        String password = "password123";
        saveUser(email, password);

        MvcResult result = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(email, password)))
            .andExpect(status().isOk())
            .andReturn();

        HttpSession session = result.getRequest().getSession(false);

        assertThat(session).isNotNull();
        assertThat(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
            .isNotNull();
    }

    @Test
    void 로그인하면_세션_식별자를_변경한다() throws Exception {
        String email = "login-session-id-" + UUID.randomUUID() + "@example.com";
        String password = "password123";
        saveUser(email, password);
        MockHttpSession session = new MockHttpSession();
        String previousSessionId = session.getId();

        MvcResult result = mockMvc.perform(post("/login")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(email, password)))
            .andExpect(status().isOk())
            .andReturn();

        HttpSession authenticatedSession = result.getRequest().getSession(false);

        assertThat(authenticatedSession).isNotNull();
        assertThat(authenticatedSession.getId()).isNotEqualTo(previousSessionId);
    }

    @Test
    void 잘못된_비밀번호로_로그인하면_실패한다() throws Exception {
        String email = "login-fail-" + UUID.randomUUID() + "@example.com";
        saveUser(email, "password123");

        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(email, "wrong-password")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 로그아웃하면_인증된_세션을_무효화한다() throws Exception {
        String password = "password123";
        User user = saveUser("logout-" + UUID.randomUUID() + "@example.com", password);
        MockHttpSession session = login(user.getEmail(), password);

        mockMvc.perform(post("/logout")
                .with(csrf())
                .session(session))
            .andExpect(status().isOk());

        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void 로그아웃은_인증이_필요하다() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 보호된_API는_인증이_필요하다() throws Exception {
        mockMvc.perform(post("/organizations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 현재_사용자_조회는_인증된_사용자_정보를_반환한다() throws Exception {
        String password = "password123";
        String nickname = "nickname-" + UUID.randomUUID().toString().substring(0, 8);
        User user = saveUser("me-" + UUID.randomUUID() + "@example.com", nickname, password);
        MockHttpSession session = login(user.getEmail(), password);

        mockMvc.perform(get("/users/me")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId().toString()))
            .andExpect(jsonPath("$.email").value(user.getEmail()))
            .andExpect(jsonPath("$.nickname").value(nickname));
    }

    @Test
    void 조직_생성은_요청_본문의_사용자_식별자_대신_인증된_사용자를_사용한다() throws Exception {
        String password = "password123";
        User authenticatedUser = saveUser("auth-" + UUID.randomUUID() + "@example.com", password);
        User spoofedUser = saveUser("spoofed-" + UUID.randomUUID() + "@example.com", password);
        MockHttpSession session = login(authenticatedUser.getEmail(), password);
        String organizationName = "org-" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/organizations")
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "%s",
                      "name": "%s",
                      "detail": "test organization"
                    }
                    """.formatted(spoofedUser.getId(), organizationName)))
            .andExpect(status().isCreated());

        Organization organization = organizationRepository.findAll().stream()
            .filter(candidate -> organizationName.equals(candidate.getName()))
            .findFirst()
            .orElseThrow();

        assertThat(userGroupRepository.existsByUserIdAndOrganizationIdAndDeletedAtIsNull(
            authenticatedUser.getId(),
            organization.getId()
        )).isTrue();
        assertThat(userGroupRepository.existsByUserIdAndOrganizationIdAndDeletedAtIsNull(
            spoofedUser.getId(),
            organization.getId()
        )).isFalse();
    }

    @Test
    void 보호된_변경_요청은_CSRF_토큰이_없으면_거부된다() throws Exception {
        String password = "password123";
        User user = saveUser("csrf-" + UUID.randomUUID() + "@example.com", password);
        MockHttpSession session = login(user.getEmail(), password);

        mockMvc.perform(post("/logout").session(session))
            .andExpect(status().isForbidden());

        assertThat(session.isInvalid()).isFalse();
    }

    private User saveUser(String email, String rawPassword) {
        return saveUser(email, "tester", rawPassword);
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
