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
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import io.github.mongsil3344.qnow.session.domain.Session;
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
class SessionPresentationControllerTest {

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
    void getSessionPresentationsReturnsUploadedPresentationTitlesAndPresenters() throws Exception {
        String password = "password123";
        User presenter = saveUser("presenter-" + UUID.randomUUID() + "@example.com", "김민준", password);
        User audience = saveUser("audience-" + UUID.randomUUID() + "@example.com", "이서연", password);
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
            .startAt(Instant.parse("2026-06-17T10:00:00Z"))
            .build());
        Session otherSession = sessionRepository.save(Session.builder()
            .organizationId(organization.getId())
            .creatorId(presenter.getId())
            .title("다른 세션")
            .startAt(Instant.parse("2026-06-18T10:00:00Z"))
            .build());

        saveUploadedPresentation(organization.getId(), session.getId(), presenter.getId(), "Qnow 발표 자료");
        savePendingPresentation(organization.getId(), session.getId(), presenter.getId(), "업로드 중인 자료");
        saveUploadedPresentation(organization.getId(), otherSession.getId(), presenter.getId(), "다른 세션 자료");

        mockMvc.perform(get("/organizations/{organizationId}/sessions/{sessionId}/presentations",
                organization.getId(),
                session.getId())
                .session(loginSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.presentations[0].presentationId").isNotEmpty())
            .andExpect(jsonPath("$.presentations[0].title").value("Qnow 발표 자료"))
            .andExpect(jsonPath("$.presentations[0].presenter").value("김민준"))
            .andExpect(jsonPath("$.presentations[0].thumbnailUrl").value(nullValue()))
            .andExpect(jsonPath("$.presentations[0].canDelete").value(false))
            .andExpect(jsonPath("$.presentations[1]").doesNotExist());
    }

    @Test
    void getSessionPresentationsReturnsThumbnailUrlWhenThumbnailKeyExists() throws Exception {
        String password = "password123";
        User presenter = saveUser("thumbnail-presenter-" + UUID.randomUUID() + "@example.com", "김민준", password);
        User audience = saveUser("thumbnail-audience-" + UUID.randomUUID() + "@example.com", "이서연", password);
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
            .startAt(Instant.parse("2026-06-17T10:00:00Z"))
            .build());
        saveUploadedPresentationWithThumbnail(organization.getId(), session.getId(), presenter.getId(), "Qnow 발표 자료");

        System.setProperty("aws.accessKeyId", "test-access-key");
        System.setProperty("aws.secretAccessKey", "test-secret-key");
        try {
            mockMvc.perform(get("/organizations/{organizationId}/sessions/{sessionId}/presentations",
                    organization.getId(),
                    session.getId())
                    .session(loginSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presentations[0].title").value("Qnow 발표 자료"))
                .andExpect(jsonPath("$.presentations[0].thumbnailUrl").isNotEmpty());
        } finally {
            System.clearProperty("aws.accessKeyId");
            System.clearProperty("aws.secretAccessKey");
        }
    }

    @Test
    void getSessionPresentationsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/organizations/{organizationId}/sessions/{sessionId}/presentations",
                UUID.randomUUID(),
                UUID.randomUUID()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getSessionPresentationsRequiresOrganizationMember() throws Exception {
        String password = "password123";
        User owner = saveUser("owner-" + UUID.randomUUID() + "@example.com", "김민준", password);
        User outsider = saveUser("outsider-" + UUID.randomUUID() + "@example.com", "박지호", password);
        MockHttpSession loginSession = login(outsider.getEmail(), password);

        Organization organization = saveOrganization();
        userGroupRepository.save(UserGroup.builder()
            .userId(owner.getId())
            .organization(organization)
            .role(UserGroupRole.ADMIN)
            .build());

        Session session = sessionRepository.save(Session.builder()
            .organizationId(organization.getId())
            .creatorId(owner.getId())
            .title("Spring 트랜잭션 전파")
            .build());

        mockMvc.perform(get("/organizations/{organizationId}/sessions/{sessionId}/presentations",
                organization.getId(),
                session.getId())
                .session(loginSession))
            .andExpect(status().isForbidden());
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

    private Presentation saveUploadedPresentation(UUID organizationId, UUID sessionId, UUID presenterId, String title) {
        Presentation presentation = createPresentation(organizationId, sessionId, presenterId, title);
        presentation.setStatusUploaded();

        return presentationRepository.save(presentation);
    }

    private Presentation saveUploadedPresentationWithThumbnail(
        UUID organizationId,
        UUID sessionId,
        UUID presenterId,
        String title
    ) {
        Presentation presentation = createPresentation(organizationId, sessionId, presenterId, title);
        presentation.assignThumbnailS3Key("%s/thumbnail.webp".formatted(
            presentation.getS3Key().replace("/original.pdf", "")
        ));
        presentation.setStatusUploaded();

        return presentationRepository.save(presentation);
    }

    private Presentation savePendingPresentation(UUID organizationId, UUID sessionId, UUID presenterId, String title) {
        return presentationRepository.save(createPresentation(organizationId, sessionId, presenterId, title));
    }

    private Presentation createPresentation(UUID organizationId, UUID sessionId, UUID presenterId, String title) {
        Presentation presentation = Presentation.builder()
            .sessionId(sessionId)
            .presenterId(presenterId)
            .title(title)
            .pageCount(12)
            .build();
        presentation.assignS3Key("presentations/%s/%s/%s/original.pdf".formatted(
            organizationId,
            sessionId,
            presentation.getId()
        ));

        return presentation;
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
