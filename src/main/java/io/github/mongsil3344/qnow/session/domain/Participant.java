package io.github.mongsil3344.qnow.session.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "participants")
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "guest_nickname", length = 30)
    private String guestNickname;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Member가 참여하는 경우
    private Participant(UUID userId, Session session) {
        this.userId = Objects.requireNonNull(userId);
        this.session = Objects.requireNonNull(session);
    }

    // Guest가 참여하는 경우
    private Participant(String guestNickname, Session session) {
        this.guestNickname = Objects.requireNonNull(guestNickname);
        this.session = Objects.requireNonNull(session);
    }

    public static Participant member(UUID userId, Session session) {
        return new Participant(userId, session);
    }

    public static Participant guest(String guestNickname, Session session) {
        return new Participant(guestNickname, session);
    }

    public void exit() {
        exit(Instant.now());
    }

    public void exit(Instant exitedAt) {
        if (deletedAt == null) {
            deletedAt = Objects.requireNonNull(exitedAt);
        }
    }

    @PrePersist
    void initialize() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
