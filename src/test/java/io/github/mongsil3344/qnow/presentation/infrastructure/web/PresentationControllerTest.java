package io.github.mongsil3344.qnow.presentation.infrastructure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.mongsil3344.qnow.organization.domain.Organization;
import io.github.mongsil3344.qnow.organization.domain.UserGroup;
import io.github.mongsil3344.qnow.organization.domain.UserGroupRole;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.OrganizationRepository;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.UserGroupRepository;
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import io.github.mongsil3344.qnow.session.domain.Session;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionRepository;
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
class PresentationControllerTest {

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
    private PresentationRepository presentationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void getPresentationPdfReturnsPresignedPdfUrl() throws Exception {
        String password = "password123";
        User presenter = saveUser("pdf-presenter-" + UUID.randomUUID() + "@example.com", "김민준", password);
        User audience = saveUser("pdf-audience-" + UUID.randomUUID() + "@example.com", "이서연", password);
        MockHttpSession loginSession = login(audience.getEmail(), password);

        Organization organization = saveOrganization();
        userGroupRepository.save(UserGroup.builder()
                .userId(presenter.getId())
                .organization(organization)
                .role(UserGroupRole.ADMIN)
                .build());
        userGroupRepository.save(UserGroup.builder()
                .userId(audience.getId())
                .organization(organization)
                .role(UserGroupRole.USER)
                .build());

        Session session = sessionRepository.save(Session.builder()
                .organizationId(organization.getId())
                .creatorId(presenter.getId())
                .title("Spring 트랜잭션 전파")
                .build());
        Presentation presentation = saveUploadedPresentation(organization.getId(), session.getId(), presenter.getId());

        System.setProperty("aws.accessKeyId", "test-access-key");
        System.setProperty("aws.secretAccessKey", "test-secret-key");
        try {
            mockMvc.perform(get("/organizations/{organizationId}/sessions/{sessionId}/presentations/{presentationId}/pdf",
                    organization.getId(),
                    session.getId(),
                    presentation.getId())
                    .session(loginSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presentationId").value(presentation.getId().toString()))
                .andExpect(jsonPath("$.pdfUrl").isNotEmpty())
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
        } finally {
            System.clearProperty("aws.accessKeyId");
            System.clearProperty("aws.secretAccessKey");
        }
    }

    @Test
    void getPresentationPdfRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/organizations/{organizationId}/sessions/{sessionId}/presentations/{presentationId}/pdf",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()))
            .andExpect(status().isUnauthorized());
    }

    private User saveUser(String email, String nickname, String rawPassword) {
        User user = User.builder()
                .email(email)
                .nickname(nickname)
                .password(passwordEncoder.encode(rawPassword))
                .build();

        return userRepository.save(user);
    }

    private Organization saveOrganization() {
        return organizationRepository.save(Organization.builder()
                .name("org-" + UUID.randomUUID().toString().substring(0, 8))
                .detail("백엔드 발표 자료를 모아 질문하고 복습하는 그룹입니다.")
                .build());
    }

    private Presentation saveUploadedPresentation(UUID organizationId, UUID sessionId, UUID presenterId) {
        Presentation presentation = Presentation.builder()
                .sessionId(sessionId)
                .presenterId(presenterId)
                .title("Qnow 발표 자료")
                .pageCount(12)
                .build();
        presentation.assignS3Key("presentations/%s/%s/%s/original.pdf".formatted(
                organizationId,
                sessionId,
                presentation.getId()
        ));
        presentation.setStatusUploaded();

        return presentationRepository.save(presentation);
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
