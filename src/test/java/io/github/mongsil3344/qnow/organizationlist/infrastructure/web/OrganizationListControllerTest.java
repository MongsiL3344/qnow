package io.github.mongsil3344.qnow.organizationlist.infrastructure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.mongsil3344.qnow.organization.domain.Organization;
import io.github.mongsil3344.qnow.organization.domain.UserGroup;
import io.github.mongsil3344.qnow.organization.domain.UserGroupRole;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.OrganizationRepository;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.UserGroupRepository;
import io.github.mongsil3344.qnow.user.domain.User;
import io.github.mongsil3344.qnow.user.infrastructure.repo.UserRepository;
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
class OrganizationListControllerTest {

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
    void getOrganizationsReturnsAuthenticatedUsersOrganizations() throws Exception {
        String password = "password123";
        User owner = saveUser("owner-" + UUID.randomUUID() + "@example.com", "김민준", password);
        User member = saveUser("member-" + UUID.randomUUID() + "@example.com", "이서연", password);
        User otherUser = saveUser("other-" + UUID.randomUUID() + "@example.com", "박지훈", password);
        MockHttpSession loginSession = login(owner.getEmail(), password);

        Organization organization = organizationRepository.save(Organization.builder()
            .name("org-" + UUID.randomUUID().toString().substring(0, 8))
            .detail("백엔드 발표 자료를 모아 질문하고 복습하는 그룹입니다.")
            .build());
        Organization otherOrganization = organizationRepository.save(Organization.builder()
            .name("other-" + UUID.randomUUID().toString().substring(0, 8))
            .detail("다른 사용자의 그룹입니다.")
            .build());

        userGroupRepository.save(UserGroup.builder()
            .userId(owner.getId())
            .organization(organization)
            .role(UserGroupRole.ADMIN)
            .build());
        userGroupRepository.save(UserGroup.builder()
            .userId(member.getId())
            .organization(organization)
            .role(UserGroupRole.USER)
            .build());
        userGroupRepository.save(UserGroup.builder()
            .userId(otherUser.getId())
            .organization(otherOrganization)
            .role(UserGroupRole.ADMIN)
            .build());

        mockMvc.perform(get("/organizations")
                .session(loginSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(organization.getId().toString()))
            .andExpect(jsonPath("$[0].name").value(organization.getName()))
            .andExpect(jsonPath("$[0].detail").value(organization.getDetail()))
            .andExpect(jsonPath("$[0].memberCount").value(2))
            .andExpect(jsonPath("$[0].activeSessionCount").doesNotExist());
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
