package io.github.mongsil3344.qnow.session.infrastructure.repo;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mongsil3344.qnow.organization.domain.Organization;
import io.github.mongsil3344.qnow.organization.infrastructure.repo.OrganizationRepository;
import io.github.mongsil3344.qnow.session.api.SessionQueryApi;
import io.github.mongsil3344.qnow.session.domain.Participant;
import io.github.mongsil3344.qnow.session.domain.Session;
import io.github.mongsil3344.qnow.user.domain.User;
import io.github.mongsil3344.qnow.user.infrastructure.repo.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ParticipantRepositoryTest {

    @Autowired
    private SessionQueryApi sessionQueryApi;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findActiveParticipantIdReturnsParticipantIdForActiveParticipantInActiveSession() {
        User user = saveUser();
        Session session = saveSession(user.getId());
        Participant participant = saveParticipant(user.getId(), session);

        assertThat(sessionQueryApi.findActiveParticipantId(session.getId(), user.getId()))
            .contains(participant.getId());
    }

    @Test
    void findActiveParticipantIdReturnsEmptyForExitedParticipant() {
        User user = saveUser();
        Session session = saveSession(user.getId());
        Participant participant = saveParticipant(user.getId(), session);
        participant.exit();
        participantRepository.saveAndFlush(participant);

        assertThat(sessionQueryApi.findActiveParticipantId(session.getId(), user.getId()))
            .isEmpty();
    }

    @Test
    void findActiveParticipantIdReturnsEmptyForParticipantInDeletedSession() {
        User user = saveUser();
        Session session = saveSession(user.getId());
        saveParticipant(user.getId(), session);

        jdbcTemplate.update(
            "update sessions set deleted_at = ? where id = ?",
            Instant.now(),
            session.getId()
        );
        entityManager.clear();

        assertThat(sessionQueryApi.findActiveParticipantId(session.getId(), user.getId()))
            .isEmpty();
    }

    private Session saveSession(UUID creatorId) {
        Organization organization = organizationRepository.saveAndFlush(Organization.builder()
            .name("org-" + UUID.randomUUID().toString().substring(0, 8))
            .build());

        return sessionRepository.saveAndFlush(Session.builder()
            .organizationId(organization.getId())
            .creatorId(creatorId)
            .title("session-" + UUID.randomUUID())
            .build());
    }

    private User saveUser() {
        return userRepository.saveAndFlush(User.builder()
            .email("participant-" + UUID.randomUUID() + "@example.com")
            .nickname("participant")
            .password("password")
            .build());
    }

    private Participant saveParticipant(UUID userId, Session session) {
        return participantRepository.saveAndFlush(Participant.builder()
            .userId(userId)
            .session(session)
            .build());
    }
}
