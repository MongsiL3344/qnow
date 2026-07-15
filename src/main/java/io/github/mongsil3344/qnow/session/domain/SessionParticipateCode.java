package io.github.mongsil3344.qnow.session.domain;

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
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "session_participate_codes")
public class SessionParticipateCode {

    private static final String CODE_CHARACTERS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_GROUP_LENGTH = 4;
    private static final int FORMATTED_CODE_LENGTH = 9;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private SessionParticipateCode(Session session) {
        this.session = Objects.requireNonNull(session);
        this.code = generateCode();
    }

    public static SessionParticipateCode create(Session session) {
        return new SessionParticipateCode(session);
    }

    public void delete() {
        delete(Instant.now());
    }

    public void delete(Instant deletedAt) {
        if (this.deletedAt == null) {
            this.deletedAt = Objects.requireNonNull(deletedAt);
        }
    }

    private static String generateCode() {
        char[] code = new char[FORMATTED_CODE_LENGTH];

        for (int index = 0; index < code.length; index++) {
            if (index == CODE_GROUP_LENGTH) {
                code[index] = '-';
                continue;
            }

            code[index] = CODE_CHARACTERS.charAt(SECURE_RANDOM.nextInt(CODE_CHARACTERS.length()));
        }

        return new String(code);
    }

    @PrePersist
    void initialize() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
