package io.github.mongsil3344.qnow.presentation.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.mongsil3344.qnow.organization.domain.Organization;
import io.github.mongsil3344.qnow.organization.domain.UserGroup;
import io.github.mongsil3344.qnow.organization.domain.UserGroupRole;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.OrganizationRepository;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.UserGroupRepository;
import io.github.mongsil3344.qnow.presentation.application.PresenterViewStateStore;
import io.github.mongsil3344.qnow.presentation.application.PresenterViewUpdateResult;
import io.github.mongsil3344.qnow.presentation.application.exception.PresenterViewUnavailableException;
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewSnapshot;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PresenterViewControllerTest {

    private static final String PASSWORD = "password123";

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
    private PresentationRepository presentationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private PresenterViewStateStore stateStore;

    @Test
    void 생성자는_제어_권한이_있는_빈_발표자_화면을_조회한다() throws Exception {
        Fixture fixture = createFixture(true, false);
        when(stateStore.get(fixture.session().getId()))
            .thenReturn(PresenterViewSnapshot.empty(fixture.session().getId()));

        mockMvc.perform(get(endpoint(), fixture.organization().getId(), fixture.session().getId())
                .session(login(fixture.creator())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canControl").value(true))
            .andExpect(jsonPath("$.sessionId").value(fixture.session().getId().toString()))
            .andExpect(jsonPath("$.presentationId").doesNotExist())
            .andExpect(jsonPath("$.pageNumber").doesNotExist())
            .andExpect(jsonPath("$.revision").value(0))
            .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }

    @Test
    void 활성_청중은_제어_권한_없이_최신_발표자_화면을_조회한다() throws Exception {
        Fixture fixture = createFixture(true, false);
        PresenterViewSnapshot snapshot = snapshot(fixture, 7, 4);
        when(stateStore.get(fixture.session().getId())).thenReturn(snapshot);

        mockMvc.perform(get(endpoint(), fixture.organization().getId(), fixture.session().getId())
                .session(login(fixture.audience())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canControl").value(false))
            .andExpect(jsonPath("$.presentationId").value(fixture.presentation().getId().toString()))
            .andExpect(jsonPath("$.pageNumber").value(7))
            .andExpect(jsonPath("$.revision").value(4));
    }

    @Test
    void 참여자가_아니면_발표자_화면을_조회할_수_없다() throws Exception {
        Fixture fixture = createFixture(false, false);

        mockMvc.perform(get(endpoint(), fixture.organization().getId(), fixture.session().getId())
                .session(login(fixture.audience())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("SESSION_PARTICIPANT_REQUIRED"));
    }

    @Test
    void 경로의_조직에_속하지_않는_세션은_찾을_수_없다() throws Exception {
        Fixture fixture = createFixture(true, false);

        mockMvc.perform(get(endpoint(), fixture.organization().getId(), UUID.randomUUID())
                .session(login(fixture.creator())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void 생성자는_발표자_화면을_변경할_수_있다() throws Exception {
        Fixture fixture = createFixture(true, false);
        PresenterViewSnapshot snapshot = snapshot(fixture, 5, 1);
        when(stateStore.update(
            org.mockito.ArgumentMatchers.eq(fixture.session().getId()),
            org.mockito.ArgumentMatchers.eq(fixture.presentation().getId()),
            org.mockito.ArgumentMatchers.eq(5),
            any(Instant.class)
        )).thenReturn(new PresenterViewUpdateResult(snapshot, true));

        mockMvc.perform(put(endpoint(), fixture.organization().getId(), fixture.session().getId())
                .with(csrf())
                .session(login(fixture.creator()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(fixture.presentation().getId(), 5)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canControl").value(true))
            .andExpect(jsonPath("$.pageNumber").value(5))
            .andExpect(jsonPath("$.revision").value(1));
    }

    @Test
    void 활성_청중은_발표자_화면을_변경할_수_없다() throws Exception {
        Fixture fixture = createFixture(true, false);

        mockMvc.perform(put(endpoint(), fixture.organization().getId(), fixture.session().getId())
                .with(csrf())
                .session(login(fixture.audience()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(fixture.presentation().getId(), 5)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PRESENTER_VIEW_CONTROL_FORBIDDEN"));
    }

    @Test
    void 세션에_속하지_않는_발표자료는_찾을_수_없다() throws Exception {
        Fixture fixture = createFixture(true, false);

        mockMvc.perform(put(endpoint(), fixture.organization().getId(), fixture.session().getId())
                .with(csrf())
                .session(login(fixture.creator()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(UUID.randomUUID(), 1)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRESENTATION_NOT_FOUND"));
    }

    @Test
    void 발표자료_범위를_벗어난_페이지는_전용_오류를_반환한다() throws Exception {
        Fixture fixture = createFixture(true, false);

        mockMvc.perform(put(endpoint(), fixture.organization().getId(), fixture.session().getId())
                .with(csrf())
                .session(login(fixture.creator()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(fixture.presentation().getId(), 0)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_PRESENTER_VIEW_PAGE"));
    }

    @Test
    void 종료된_세션은_발표자_화면_조회를_거부한다() throws Exception {
        Fixture fixture = createFixture(true, true);

        mockMvc.perform(get(endpoint(), fixture.organization().getId(), fixture.session().getId())
                .session(login(fixture.creator())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SESSION_ENDED"));
    }

    @Test
    void 레디스_장애는_서비스_사용_불가를_반환한다() throws Exception {
        Fixture fixture = createFixture(true, false);
        when(stateStore.get(fixture.session().getId()))
            .thenThrow(new PresenterViewUnavailableException("발표 화면 동기화 서비스를 사용할 수 없습니다"));

        mockMvc.perform(get(endpoint(), fixture.organization().getId(), fixture.session().getId())
                .session(login(fixture.creator())))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("PRESENTER_VIEW_UNAVAILABLE"));
    }

    @Test
    void 발표자_화면은_인증과_CSRF_토큰이_필요하다() throws Exception {
        Fixture fixture = createFixture(true, false);

        mockMvc.perform(get(endpoint(), fixture.organization().getId(), fixture.session().getId()))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(put(endpoint(), fixture.organization().getId(), fixture.session().getId())
                .session(login(fixture.creator()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(fixture.presentation().getId(), 1)))
            .andExpect(status().isForbidden());
    }

    private Fixture createFixture(boolean audienceParticipates, boolean ended) {
        User creator = saveUser("creator");
        User audience = saveUser("audience");
        Organization organization = organizationRepository.save(Organization.builder()
            .name("org-" + UUID.randomUUID().toString().substring(0, 8))
            .detail("발표자 화면 테스트 조직")
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

        Session session = sessionRepository.save(Session.builder()
            .organizationId(organization.getId())
            .creatorId(creator.getId())
            .title("session-" + UUID.randomUUID())
            .startAt(Instant.parse("2026-07-13T09:00:00Z"))
            .endAt(ended ? Instant.parse("2026-07-13T11:00:00Z") : null)
            .build());
        participantRepository.save(Participant.member(creator.getId(), session));
        if (audienceParticipates) {
            participantRepository.save(Participant.member(audience.getId(), session));
        }

        Presentation presentation = Presentation.builder()
            .sessionId(session.getId())
            .presenterId(creator.getId())
            .title("Qnow 발표 자료")
            .pageCount(12)
            .build();
        presentation.assignS3Key("presentations/%s/%s/original.pdf".formatted(
            session.getId(),
            presentation.getId()
        ));
        presentation.setStatusUploaded();
        presentationRepository.save(presentation);

        return new Fixture(creator, audience, organization, session, presentation);
    }

    private User saveUser(String prefix) {
        return userRepository.save(User.builder()
            .email("%s-%s@example.com".formatted(prefix, UUID.randomUUID()))
            .nickname(prefix)
            .password(passwordEncoder.encode(PASSWORD))
            .build());
    }

    private MockHttpSession login(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(user.getEmail(), PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private PresenterViewSnapshot snapshot(Fixture fixture, int pageNumber, long revision) {
        return new PresenterViewSnapshot(
            fixture.session().getId(),
            fixture.presentation().getId(),
            pageNumber,
            revision,
            Instant.parse("2026-07-13T10:20:30Z")
        );
    }

    private String endpoint() {
        return "/organizations/{organizationId}/sessions/{sessionId}/presenter-view";
    }

    private String updateBody(UUID presentationId, int pageNumber) {
        return """
            {"presentationId":"%s","pageNumber":%d}
            """.formatted(presentationId, pageNumber);
    }

    private record Fixture(
        User creator,
        User audience,
        Organization organization,
        Session session,
        Presentation presentation
    ) {
    }
}
