package io.github.mongsil3344.qnow.question.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "presentation_id", nullable = false)
    private UUID presentationId;

    @Column(name = "questioner_id", nullable = false)
    private UUID questionerId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionKind kind = QuestionKind.QUESTION;

    @Column(name = "is_anonymous", nullable = false)
    private boolean anonymous;

    @Column(name = "page_start", nullable = false)
    private int pageStart;

    @Column(name = "page_end", nullable = false)
    private int pageEnd;

    @Column(name = "upvote_count", nullable = false)
    private int upvoteCount;

    @Embedded
    private QuestionSelection selection;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Builder
    private Question(
            UUID presentationId,
            UUID questionerId,
            String content,
            boolean anonymous,
            int pageStart,
            int pageEnd,
            QuestionSelection selection,
            QuestionKind kind
    ) {
        this.presentationId = presentationId;
        this.questionerId = questionerId;
        this.content = content;
        this.kind = kind == null ? QuestionKind.QUESTION : kind;
        this.anonymous = anonymous;
        this.pageStart = pageStart;
        this.pageEnd = pageEnd;
        this.selection = selection;
        this.upvoteCount = 0;
    }

    public static Question controlRequest(
            UUID presentationId,
            UUID questionerId,
            int pageNumber
    ) {
        return Question.builder()
                .presentationId(presentationId)
                .questionerId(questionerId)
                .content("")
                .anonymous(false)
                .pageStart(pageNumber)
                .pageEnd(pageNumber)
                .kind(QuestionKind.CONTROL_REQUEST)
                .build();
    }

    public void incrementUpvoteCount() {
        upvoteCount++;
    }

    public void decrementUpvoteCount() {
        if (upvoteCount <= 0) {
            throw new IllegalStateException("upvoteCount must not be negative");
        }

        upvoteCount--;
    }

    public void delete() {
        if (deletedAt == null) {
            deletedAt = Instant.now();
        }
    }

    public void approve(Instant approvalTime) {
        if (approvedAt == null) {
            approvedAt = Objects.requireNonNull(approvalTime);
        }
    }

    @PrePersist
    void initialize() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
