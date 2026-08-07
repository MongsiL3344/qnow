package io.github.mongsil3344.qnow.presentation.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import io.github.mongsil3344.qnow.presentation.application.PresenterControlStore;
import io.github.mongsil3344.qnow.presentation.application.exception.PresenterViewUnavailableException;
import io.github.mongsil3344.qnow.presentation.domain.Presentation;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewSnapshot;
import io.github.mongsil3344.qnow.presentation.infrastructure.repo.PresentationRepository;
import io.github.mongsil3344.qnow.session.api.GuestPrincipal;
import io.github.mongsil3344.qnow.session.domain.Participant;
import io.github.mongsil3344.qnow.session.domain.Session;
import io.github.mongsil3344.qnow.session.infrastructure.repo.ParticipantRepository;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionRepository;
import io.github.mongsil3344.qnow.user.domain.User;
import io.github.mongsil3344.qnow.user.infrastructure.repo.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
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

    @MockitoBean
    private PresenterControlStore controlStore;

    @BeforeEach
    void setUpControlStore() {
        when(controlStore.getExpiry(any(UUID.class), any(UUID.class))).thenReturn(Optional.empty());
    }

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
            .andExpect(jsonPath("$.sequence").value(0))
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
            .andExpect(jsonPath("$.sequence").value(4));
    }

    @Test
    void 활성_비회원은_제어_권한_없이_최신_발표자_화면을_조회한다() throws Exception {
        Fixture fixture = createFixture(false, false);
        Participant guest = participantRepository.save(Participant.guest("guest", fixture.session()));
        PresenterViewSnapshot snapshot = snapshot(fixture, 7, 4);
        when(stateStore.get(fixture.session().getId())).thenReturn(snapshot);

        mockMvc.perform(get(endpoint(), fixture.organization().getId(), fixture.session().getId())
                .session(guestSession(guest, fixture.session())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canControl").value(false))
            .andExpect(jsonPath("$.presentationId").value(fixture.presentation().getId().toString()))
            .andExpect(jsonPath("$.pageNumber").value(7));
    }

    @Test
    void 비회원은_발표자_화면을_변경할_수_없다() throws Exception {
        Fixture fixture = createFixture(false, false);
        Participant guest = participantRepository.save(Participant.guest("guest", fixture.session()));

        mockMvc.perform(put(endpoint(), fixture.organization().getId(), fixture.session().getId())
                .with(csrf())
                .session(guestSession(guest, fixture.session()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(fixture.presentation().getId(), 5)))
            .andExpect(status().isForbidden());
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
        )).thenReturn(snapshot);

        mockMvc.perform(put(endpoint(), fixture.organization().getId(), fixture.session().getId())
                .with(csrf())
                .session(login(fixture.creator()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(fixture.presentation().getId(), 5)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canControl").value(true))
            .andExpect(jsonPath("$.pageNumber").value(5))
            .andExpect(jsonPath("$.sequence").value(1));
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

    @Test
    void 개설자가_참여자에게_제어권을_부여한다() throws Exception {
        Fixture fixture = createFixture(true, false);
        UUID participantId = participantRepository
            .findByUserIdAndSessionIdAndDeletedAtIsNull(
                fixture.audience().getId(),
                fixture.session().getId()
            )
            .orElseThrow()
            .getId();

        mockMvc.perform(put(controllerEndpoint(), fixture.organization().getId(), fixture.session().getId())
                .with(csrf())
                .session(login(fixture.creator()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(grantBody(participantId)))
            .andExpect(status().isNoContent());
    }

    @Test
    void 개설자가_아니면_제어권을_부여할_수_없다() throws Exception {
        Fixture fixture = createFixture(true, false);
        UUID participantId = participantRepository
            .findByUserIdAndSessionIdAndDeletedAtIsNull(
                fixture.creator().getId(),
                fixture.session().getId()
            )
            .orElseThrow()
            .getId();

        mockMvc.perform(put(controllerEndpoint(), fixture.organization().getId(), fixture.session().getId())
                .with(csrf())
                .session(login(fixture.audience()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(grantBody(participantId)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PRESENTER_VIEW_CONTROL_FORBIDDEN"));
    }

    @Test
    void 제어권을_받은_참여자가_페이지를_변경할_수_있다() throws Exception {
        Fixture fixture = createFixture(true, false);
        UUID participantId = participantRepository
            .findByUserIdAndSessionIdAndDeletedAtIsNull(
                fixture.audience().getId(),
                fixture.session().getId()
            )
            .orElseThrow()
            .getId();
        Instant expiresAt = Instant.parse("2026-08-05T12:00:00Z");
        when(controlStore.getExpiry(fixture.session().getId(), participantId)).thenReturn(Optional.of(expiresAt));
        when(stateStore.update(
            org.mockito.ArgumentMatchers.eq(fixture.session().getId()),
            org.mockito.ArgumentMatchers.eq(fixture.presentation().getId()),
            org.mockito.ArgumentMatchers.eq(5),
            any(Instant.class)
        )).thenReturn(snapshot(fixture, 5, 1));

        mockMvc.perform(put(endpoint(), fixture.organization().getId(), fixture.session().getId())
                .with(csrf())
                .session(login(fixture.audience()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(fixture.presentation().getId(), 5)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canControl").value(true));
    }

    @Test
    void 제어권을_받은_게스트가_페이지를_변경할_수_있다() throws Exception {
        Fixture fixture = createFixture(false, false);
        Participant guest = participantRepository.save(Participant.guest("guest", fixture.session()));
        Instant expiresAt = Instant.parse("2026-08-05T12:00:00Z");
        when(controlStore.getExpiry(fixture.session().getId(), guest.getId())).thenReturn(Optional.of(expiresAt));
        when(stateStore.update(
            org.mockito.ArgumentMatchers.eq(fixture.session().getId()),
            org.mockito.ArgumentMatchers.eq(fixture.presentation().getId()),
            org.mockito.ArgumentMatchers.eq(5),
            any(Instant.class)
        )).thenReturn(snapshot(fixture, 5, 1));

        mockMvc.perform(put(endpoint(), fixture.organization().getId(), fixture.session().getId())
                .with(csrf())
                .session(guestSession(guest, fixture.session()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(fixture.presentation().getId(), 5)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canControl").value(true));
    }

    @Test
    void 위임받은_참여자는_자신의_제어권만_해제할_수_있다() throws Exception {
        Fixture fixture = createFixture(true, false);
        UUID participantId = participantRepository
            .findByUserIdAndSessionIdAndDeletedAtIsNull(
                fixture.audience().getId(),
                fixture.session().getId()
            )
            .orElseThrow()
            .getId();

        mockMvc.perform(delete(controllerEndpoint(), fixture.organization().getId(), fixture.session().getId())
                .with(csrf())
                .session(login(fixture.audience()))
                .queryParam("participantId", participantId.toString()))
            .andExpect(status().isNoContent());

        mockMvc.perform(delete(controllerEndpoint(), fixture.organization().getId(), fixture.session().getId())
                .with(csrf())
                .session(login(fixture.audience()))
                .queryParam("participantId", fixture.session().getCreatorId().toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    void 조회_응답에_제어_만료시각이_포함된다() throws Exception {
        Fixture fixture = createFixture(true, false);
        UUID participantId = participantRepository
            .findByUserIdAndSessionIdAndDeletedAtIsNull(
                fixture.audience().getId(),
                fixture.session().getId()
            )
            .orElseThrow()
            .getId();
        Instant expiresAt = Instant.parse("2026-08-05T12:00:00Z");
        when(controlStore.getExpiry(fixture.session().getId(), participantId)).thenReturn(Optional.of(expiresAt));
        when(stateStore.get(fixture.session().getId())).thenReturn(PresenterViewSnapshot.empty(fixture.session().getId()));

        mockMvc.perform(get(endpoint(), fixture.organization().getId(), fixture.session().getId())
                .session(login(fixture.audience())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canControl").value(true))
            .andExpect(jsonPath("$.controlExpiresAt").value(expiresAt.toString()));
    }

    @Test
    void 비참여자에게는_제어권을_부여할_수_없다() throws Exception {
        Fixture fixture = createFixture(true, false);

        mockMvc.perform(put(controllerEndpoint(), fixture.organization().getId(), fixture.session().getId())
                .with(csrf())
                .session(login(fixture.creator()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(grantBody(UUID.randomUUID())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("PRESENTER_CONTROL_TARGET_INVALID"));
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
            .nickname(prefix + "-" + UUID.randomUUID().toString().substring(0, 8))
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

    private MockHttpSession guestSession(Participant participant, Session session) {
        MockHttpSession httpSession = new MockHttpSession();
        GuestPrincipal principal = new GuestPrincipal(participant.getId(), session.getId());
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_GUEST"))
        );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        httpSession.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            securityContext
        );
        return httpSession;
    }

    private PresenterViewSnapshot snapshot(Fixture fixture, int pageNumber, long sequence) {
        return new PresenterViewSnapshot(
            fixture.session().getId(),
            fixture.presentation().getId(),
            pageNumber,
            sequence,
            Instant.parse("2026-07-13T10:20:30Z")
        );
    }

    private String endpoint() {
        return "/organizations/{organizationId}/sessions/{sessionId}/presenter-view";
    }

    private String controllerEndpoint() {
        return "/organizations/{organizationId}/sessions/{sessionId}/presenter-view/controller";
    }

    private String updateBody(UUID presentationId, int pageNumber) {
        return """
            {"presentationId":"%s","pageNumber":%d}
            """.formatted(presentationId, pageNumber);
    }

    private String grantBody(UUID participantId) {
        return """
            {"participantId":"%s"}
            """.formatted(participantId);
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
