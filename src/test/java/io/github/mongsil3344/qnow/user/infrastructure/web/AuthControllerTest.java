package io.github.mongsil3344.qnow.user.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void loginSavesSecurityContextInSession() throws Exception {
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
    void loginFailsWithInvalidPassword() throws Exception {
        String email = "login-fail-" + UUID.randomUUID() + "@example.com";
        saveUser(email, "password123");

        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(email, "wrong-password")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedApiRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createOrganizationUsesAuthenticatedUserInsteadOfRequestBodyUserId() throws Exception {
        String password = "password123";
        User authenticatedUser = saveUser("auth-" + UUID.randomUUID() + "@example.com", password);
        User spoofedUser = saveUser("spoofed-" + UUID.randomUUID() + "@example.com", password);
        MockHttpSession session = login(authenticatedUser.getEmail(), password);
        String organizationName = "org-" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/organizations")
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
