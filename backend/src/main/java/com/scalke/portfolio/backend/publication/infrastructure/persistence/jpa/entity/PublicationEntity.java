package com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.entity;

import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Length;

import java.time.Instant;

/**
 * Structure de persistance d'une publication ({@code V006}). Les invariants vivent dans
 * {@code domain.model.Publication} et dans les contraintes de la table (ADR 0001).
 * <p>
 * {@code createdAt} et {@code updatedAt} sont fixés par l'application à partir de l'horloge applicative
 * (D-AM), jamais par la base ni par Hibernate.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "publication")
public class PublicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PublicationType type;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "slug", nullable = false, length = 160)
    private String slug;

    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @Column(name = "content_markdown", nullable = false, length = Length.LONG32, columnDefinition = "TEXT")
    private String contentMarkdown;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PublicationStatus status;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "featured", nullable = false)
    private boolean featured;

    @Column(name = "seo_title", length = 120)
    private String seoTitle;

    @Column(name = "seo_description", length = 300)
    private String seoDescription;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    private PublicationEntity(PublicationType type, String title, String slug, String summary,
                              String contentMarkdown, PublicationStatus status, Instant publishedAt,
                              boolean featured, String seoTitle, String seoDescription,
                              Instant createdAt, Instant updatedAt) {
        this.type = type;
        this.title = title;
        this.slug = slug;
        this.summary = summary;
        this.contentMarkdown = contentMarkdown;
        this.status = status;
        this.publishedAt = publishedAt;
        this.featured = featured;
        this.seoTitle = seoTitle;
        this.seoDescription = seoDescription;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
