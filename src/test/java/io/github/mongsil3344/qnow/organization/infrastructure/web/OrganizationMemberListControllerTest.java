package io.github.mongsil3344.qnow.organization.infrastructure.web;

import static org.hamcrest.Matchers.contains;
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
import java.time.ZoneId;
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
class OrganizationMemberListControllerTest {

    private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");

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
    void 조직_멤버_목록은_기본_20개_슬라이스와_요청한_필드만_반환한다() throws Exception {
        String password = "password123";
        User owner = saveUser("member-list-owner", "김민준", password);
        User member = saveUser("member-list-member", "이서연", password);
        User otherUser = saveUser("member-list-other", "박지훈", password);
        MockHttpSession loginSession = login(owner.getEmail(), password);
        Organization organization = saveOrganization("member-list");
        Organization otherOrganization = saveOrganization("member-list-other");
        UserGroup ownerMembership = saveMembership(owner, organization, UserGroupRole.ADMIN);
        UserGroup memberMembership = saveMembership(member, organization, UserGroupRole.USER);
        saveMembership(otherUser, otherOrganization, UserGroupRole.ADMIN);

        mockMvc.perform(get("/organizations/{organizationId}/members", organization.getId())
                .session(loginSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.totalElements").doesNotExist())
            .andExpect(jsonPath("$.totalPages").doesNotExist())
            .andExpect(jsonPath("$.first").doesNotExist())
            .andExpect(jsonPath("$.last").doesNotExist())
            .andExpect(jsonPath(
                "$.content[?(@.nickname == '%s')].role".formatted(owner.getNickname())
            ).value(contains("ADMIN")))
            .andExpect(jsonPath(
                "$.content[?(@.nickname == '%s')].joinedAt".formatted(owner.getNickname())
            ).value(contains(ownerMembership.getCreatedAt().atZone(SERVICE_ZONE_ID).toLocalDate().toString())))
            .andExpect(jsonPath(
                "$.content[?(@.nickname == '%s')].role".formatted(member.getNickname())
            ).value(contains("MEMBER")))
            .andExpect(jsonPath(
                "$.content[?(@.nickname == '%s')].joinedAt".formatted(member.getNickname())
            ).value(contains(memberMembership.getCreatedAt().atZone(SERVICE_ZONE_ID).toLocalDate().toString())))
            .andExpect(jsonPath("$.content[0].id").doesNotExist())
            .andExpect(jsonPath("$.content[0].email").doesNotExist());
    }

    @Test
    void 조직_멤버_목록은_쿼리스트링으로_페이징한다() throws Exception {
        String password = "password123";
        User owner = saveUser("member-page-owner", "최유진", password);
        User member = saveUser("member-page-member", "정하준", password);
        MockHttpSession loginSession = login(owner.getEmail(), password);
        Organization organization = saveOrganization("member-page");
        saveMembership(owner, organization, UserGroupRole.ADMIN);
        saveMembership(member, organization, UserGroupRole.USER);

        mockMvc.perform(get("/organizations/{organizationId}/members", organization.getId())
                .param("page", "0")
                .param("size", "1")
                .session(loginSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].role").value("ADMIN"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.totalElements").doesNotExist())
            .andExpect(jsonPath("$.totalPages").doesNotExist());
    }

    @Test
    void 조직에_가입하지_않은_사용자는_멤버_목록을_조회할_수_없다() throws Exception {
        String password = "password123";
        User owner = saveUser("member-access-owner", "강서준", password);
        User outsider = saveUser("member-access-outsider", "윤지우", password);
        MockHttpSession loginSession = login(outsider.getEmail(), password);
        Organization organization = saveOrganization("member-access");
        saveMembership(owner, organization, UserGroupRole.ADMIN);

        mockMvc.perform(get("/organizations/{organizationId}/members", organization.getId())
                .session(loginSession))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ORGANIZATION_MEMBER_REQUIRED"));
    }

    @Test
    void 조직_멤버_목록은_잘못된_페이징_조건을_거부한다() throws Exception {
        String password = "password123";
        User owner = saveUser("member-invalid-page-owner", "송도윤", password);
        MockHttpSession loginSession = login(owner.getEmail(), password);
        Organization organization = saveOrganization("member-invalid-page");
        saveMembership(owner, organization, UserGroupRole.ADMIN);

        mockMvc.perform(get("/organizations/{organizationId}/members", organization.getId())
                .param("page", "-1")
                .session(loginSession))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ORGANIZATION_MEMBER_LIST_QUERY"));
    }

    private User saveUser(String emailPrefix, String nickname, String rawPassword) {
        String uniqueValue = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(User.builder()
            .email(emailPrefix + "-" + uniqueValue + "@example.com")
            .nickname(nickname + "-" + uniqueValue)
            .password(passwordEncoder.encode(rawPassword))
            .build());
    }

    private Organization saveOrganization(String namePrefix) {
        return organizationRepository.save(Organization.builder()
            .name(namePrefix + "-" + UUID.randomUUID().toString().substring(0, 8))
            .build());
    }

    private UserGroup saveMembership(User user, Organization organization, UserGroupRole role) {
        return userGroupRepository.save(UserGroup.builder()
            .userId(user.getId())
            .organization(organization)
            .role(role)
            .build());
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
