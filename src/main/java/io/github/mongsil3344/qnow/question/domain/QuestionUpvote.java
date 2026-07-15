package io.github.mongsil3344.qnow.question.domain;

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
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "question_upvotes")
public class QuestionUpvote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "voter_user_id")
    private UUID voterUserId;

    @Column(name = "voter_guest_participant_id")
    private UUID voterGuestParticipantId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private QuestionUpvote(Question question, UUID voterUserId, UUID voterGuestParticipantId) {
        this.question = Objects.requireNonNull(question);
        this.voterUserId = voterUserId;
        this.voterGuestParticipantId = voterGuestParticipantId;
    }

    public static QuestionUpvote member(Question question, UUID voterUserId) {
        return new QuestionUpvote(question, Objects.requireNonNull(voterUserId), null);
    }

    public static QuestionUpvote guest(Question question, UUID voterGuestParticipantId) {
        return new QuestionUpvote(question, null, Objects.requireNonNull(voterGuestParticipantId));
    }

    @PrePersist
    void initialize() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
