package io.github.mongsil3344.qnow.session.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "presentations")
public class Presentation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presenter_id", nullable = false)
    private Participant presenter;

    @Column(name = "presentation_order")
    private Integer presentationOrder;

    @Column(nullable = false, length = 255)
    private String title;

    @Builder
    private Presentation(Session session, Participant presenter, Integer presentationOrder, String title) {
        this.session = session;
        this.presenter = presenter;
        this.presentationOrder = presentationOrder;
        this.title = title;
    }
}
