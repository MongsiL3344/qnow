package io.github.mongsil3344.qnow.presentation.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mongsil3344.qnow.organization.domain.Organization;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.OrganizationRepository;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewClearReason;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewEvent;
import io.github.mongsil3344.qnow.presentation.domain.PresenterViewEventType;
import io.github.mongsil3344.qnow.session.domain.Participant;
import io.github.mongsil3344.qnow.session.domain.Session;
import io.github.mongsil3344.qnow.session.infrastructure.repo.ParticipantRepository;
import io.github.mongsil3344.qnow.session.infrastructure.repo.SessionRepository;
import io.github.mongsil3344.qnow.user.domain.User;
import io.github.mongsil3344.qnow.user.infrastructure.repo.UserRepository;
import java.lang.reflect.Type;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import tools.jackson.databind.ObjectMapper;

@Import(PresenterViewWebSocketIntegrationTest.SubscriptionProbeConfiguration.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "qnow.presenter-view.realtime-enabled=false",
        "qnow.websocket.allowed-origins=http://localhost:3000"
    }
)
class PresenterViewWebSocketIntegrationTest {

    private static final String PASSWORD = "password123";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubscriptionProbe subscriptionProbe;

    private final List<StompSession> sessions = new ArrayList<>();
    private final List<WebSocketStompClient> clients = new ArrayList<>();
    private final List<ThreadPoolTaskScheduler> schedulers = new ArrayList<>();

    @AfterEach
    void disconnectClients() {
        sessions.stream()
            .filter(StompSession::isConnected)
            .forEach(StompSession::disconnect);
        clients.forEach(WebSocketStompClient::stop);
        schedulers.forEach(ThreadPoolTaskScheduler::shutdown);
    }

    @Test
    void 인증된_활성_참여자는_CSRF_토큰으로_연결하고_변경_이벤트를_수신한다() throws Exception {
        Fixture fixture = createFixture(true);
        RecordingSessionHandler sessionHandler = new RecordingSessionHandler();
        StompSession stompSession = connect(fixture.audience(), sessionHandler);
        BlockingQueue<byte[]> messages = new LinkedBlockingQueue<>();
        subscribeAndAwaitReceipt(stompSession, topic(fixture.session().getId()), messages);

        PresenterViewEvent event = new PresenterViewEvent(
            PresenterViewEventType.PRESENTER_VIEW_UPDATED,
            fixture.session().getId(),
            UUID.randomUUID(),
            8,
            3,
            Instant.parse("2026-07-13T10:20:30Z"),
            null
        );
        messagingTemplate.convertAndSend(topic(fixture.session().getId()), event);

        byte[] payload = messages.poll(5, TimeUnit.SECONDS);
        assertThat(payload).isNotNull();
        assertThat(objectMapper.readValue(payload, PresenterViewEvent.class)).isEqualTo(event);
        assertThat(sessionHandler.hasError()).isFalse();
    }

    @Test
    void 참여자가_아닌_사용자의_구독은_거부된다() throws Exception {
        Fixture fixture = createFixture(false);
        RecordingSessionHandler sessionHandler = new RecordingSessionHandler();
        StompSession stompSession = connect(fixture.audience(), sessionHandler);

        stompSession.subscribe(topic(fixture.session().getId()), new ByteArrayFrameHandler(
            new LinkedBlockingQueue<>()
        ));

        assertThat(sessionHandler.awaitError()).isTrue();
    }

    @Test
    void 클라이언트의_토픽_전송은_거부된다() throws Exception {
        Fixture fixture = createFixture(true);
        RecordingSessionHandler sessionHandler = new RecordingSessionHandler();
        StompSession stompSession = connect(fixture.audience(), sessionHandler);

        stompSession.send(topic(fixture.session().getId()), new byte[0]);

        assertThat(sessionHandler.awaitError()).isTrue();
    }

    @Test
    void 다른_세션의_이벤트는_구독자에게_노출되지_않는다() throws Exception {
        Fixture fixture = createFixture(true);
        UUID otherSessionId = UUID.randomUUID();
        RecordingSessionHandler sessionHandler = new RecordingSessionHandler();
        StompSession stompSession = connect(fixture.audience(), sessionHandler);
        BlockingQueue<byte[]> messages = new LinkedBlockingQueue<>();
        subscribeAndAwaitReceipt(stompSession, topic(fixture.session().getId()), messages);

        messagingTemplate.convertAndSend(topic(otherSessionId), clearEvent(otherSessionId, 1));
        assertThat(messages.poll(500, TimeUnit.MILLISECONDS)).isNull();

        PresenterViewEvent expected = clearEvent(fixture.session().getId(), 2);
        messagingTemplate.convertAndSend(topic(fixture.session().getId()), expected);
        byte[] payload = messages.poll(5, TimeUnit.SECONDS);

        assertThat(payload).isNotNull();
        assertThat(objectMapper.readValue(payload, PresenterViewEvent.class)).isEqualTo(expected);
    }

    private StompSession connect(User user, RecordingSessionHandler sessionHandler) throws Exception {
        AuthenticatedHttpSession authenticated = loginAndGetCsrf(user);
        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        handshakeHeaders.setOrigin("http://localhost:3000");
        handshakeHeaders.add(HttpHeaders.COOKIE, authenticated.cookieHeader());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("X-CSRF-TOKEN", authenticated.csrfToken());

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("presenter-view-stomp-test-");
        scheduler.initialize();
        schedulers.add(scheduler);

        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setTaskScheduler(scheduler);
        client.start();
        clients.add(client);

        StompSession session = client.connectAsync(
            URI.create("ws://localhost:%d/ws".formatted(port)),
            handshakeHeaders,
            connectHeaders,
            sessionHandler
        ).get(5, TimeUnit.SECONDS);
        sessions.add(session);
        return session;
    }

    private AuthenticatedHttpSession loginAndGetCsrf(User user) throws Exception {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient httpClient = HttpClient.newBuilder().cookieHandler(cookieManager).build();

        HttpResponse<String> loginResponse = httpClient.send(
            HttpRequest.newBuilder(URI.create("http://localhost:%d/login".formatted(port)))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString("""
                    {"email":"%s","password":"%s"}
                    """.formatted(user.getEmail(), PASSWORD)))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertThat(loginResponse.statusCode()).isEqualTo(200);

        HttpResponse<String> csrfResponse = httpClient.send(
            HttpRequest.newBuilder(URI.create("http://localhost:%d/csrf".formatted(port)))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertThat(csrfResponse.statusCode()).isEqualTo(200);

        String cookieHeader = cookieManager.getCookieStore().getCookies().stream()
            .map(cookie -> cookie.getName() + "=" + cookie.getValue())
            .collect(java.util.stream.Collectors.joining("; "));
        String csrfToken = objectMapper.readTree(csrfResponse.body()).path("token").asText();
        assertThat(cookieHeader).contains("JSESSIONID=");
        assertThat(csrfToken).isNotBlank();
        return new AuthenticatedHttpSession(cookieHeader, csrfToken);
    }

    private void subscribeAndAwaitReceipt(
        StompSession session,
        String destination,
        BlockingQueue<byte[]> messages
    ) throws InterruptedException {
        session.subscribe(
            destination,
            new ByteArrayFrameHandler(messages)
        );
        assertThat(subscriptionProbe.await(destination)).isTrue();
    }

    private Fixture createFixture(boolean audienceParticipates) {
        User creator = saveUser("creator");
        User audience = saveUser("audience");
        Organization organization = organizationRepository.save(Organization.builder()
            .name("org-" + UUID.randomUUID().toString().substring(0, 8))
            .detail("WebSocket 통합 테스트 조직")
            .build());
        Session session = sessionRepository.save(Session.builder()
            .organizationId(organization.getId())
            .creatorId(creator.getId())
            .title("session-" + UUID.randomUUID())
            .startAt(Instant.parse("2026-07-13T09:00:00Z"))
            .build());
        participantRepository.save(Participant.member(creator.getId(), session));
        if (audienceParticipates) {
            participantRepository.save(Participant.member(audience.getId(), session));
        }
        return new Fixture(audience, session);
    }

    private User saveUser(String prefix) {
        return userRepository.save(User.builder()
            .email("%s-%s@example.com".formatted(prefix, UUID.randomUUID()))
            .nickname(prefix)
            .password(passwordEncoder.encode(PASSWORD))
            .build());
    }

    private PresenterViewEvent clearEvent(UUID sessionId, long sequence) {
        return new PresenterViewEvent(
            PresenterViewEventType.PRESENTER_VIEW_CLEARED,
            sessionId,
            null,
            null,
            sequence,
            Instant.parse("2026-07-13T10:20:30Z"),
            PresenterViewClearReason.SESSION_ENDED
        );
    }

    private String topic(UUID sessionId) {
        return "/topic/sessions/%s/presenter-view".formatted(sessionId);
    }

    private record Fixture(User audience, Session session) {
    }

    private record AuthenticatedHttpSession(String cookieHeader, String csrfToken) {
    }

    @TestConfiguration
    static class SubscriptionProbeConfiguration {

        @Bean
        SubscriptionProbe subscriptionProbe() {
            return new SubscriptionProbe();
        }
    }

    static final class SubscriptionProbe {

        private final BlockingQueue<String> destinations = new LinkedBlockingQueue<>();

        @EventListener
        void onSubscribe(SessionSubscribeEvent event) {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
            if (accessor.getDestination() != null) {
                destinations.add(accessor.getDestination());
            }
        }

        boolean await(String expectedDestination) throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < deadline) {
                long remaining = deadline - System.nanoTime();
                String destination = destinations.poll(remaining, TimeUnit.NANOSECONDS);
                if (expectedDestination.equals(destination)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class ByteArrayFrameHandler implements StompFrameHandler {

        private final BlockingQueue<byte[]> messages;

        private ByteArrayFrameHandler(BlockingQueue<byte[]> messages) {
            this.messages = messages;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return byte[].class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            messages.add((byte[]) payload);
        }
    }

    private static final class RecordingSessionHandler extends StompSessionHandlerAdapter {

        private final CountDownLatch error = new CountDownLatch(1);

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return byte[].class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            error.countDown();
        }

        @Override
        public void handleException(
            StompSession session,
            StompCommand command,
            StompHeaders headers,
            byte[] payload,
            Throwable exception
        ) {
            error.countDown();
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            error.countDown();
        }

        boolean awaitError() throws InterruptedException {
            return error.await(5, TimeUnit.SECONDS);
        }

        boolean hasError() {
            return error.getCount() == 0;
        }
    }
}
