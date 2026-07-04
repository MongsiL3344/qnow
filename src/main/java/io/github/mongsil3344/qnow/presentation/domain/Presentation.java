package io.github.mongsil3344.qnow.presentation.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "presentations")
public class Presentation implements Persistable<UUID> {

    private static final String DEFAULT_CONTENT_TYPE = "application/pdf";

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "presenter_id", nullable = false)
    private UUID presenterId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "s3_key", nullable = false, unique = true, length = 1024)
    private String s3Key;

    @Column(name = "thumbnail_s3_key", unique = true, length = 1024)
    private String thumbnailS3Key;

    @Column(name = "page_count", nullable = false)
    private int pageCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false, length = 20)
    private UploadStatus uploadStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Transient
    private boolean newEntity = true;

    @Builder
    private Presentation(
            UUID sessionId,
            UUID presenterId,
            String title,
            int pageCount
    ) {
        if (pageCount < 1) {
            throw new IllegalArgumentException("pageCount must be positive");
        }

        this.id = UUID.randomUUID();
        this.sessionId = sessionId;
        this.presenterId = presenterId;
        this.title = title;
        this.pageCount = pageCount;
        this.contentType = DEFAULT_CONTENT_TYPE;
        this.uploadStatus = UploadStatus.PENDING;
    }

    public void assignS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public void assignThumbnailS3Key(String thumbnailS3Key) {
        this.thumbnailS3Key = thumbnailS3Key;
    }

    public void clearThumbnailS3Key() {
        this.thumbnailS3Key = null;
    }

    public void setStatusUploaded() {
        this.uploadStatus = UploadStatus.UPLOADED;
    }

    public void setStatusFailed() {
        this.uploadStatus = UploadStatus.FAILED;
    }

    public void delete() {
        if (deletedAt == null) {
            deletedAt = Instant.now();
        }
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PrePersist
    void initialize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (contentType == null || contentType.isBlank()) {
            contentType = DEFAULT_CONTENT_TYPE;
        }
        if (uploadStatus == null) {
            uploadStatus = UploadStatus.PENDING;
        }
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        newEntity = false;
    }
}
