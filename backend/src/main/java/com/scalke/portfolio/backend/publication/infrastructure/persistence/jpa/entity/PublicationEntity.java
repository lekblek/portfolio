package com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.entity;

import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Length;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Structure de persistance d'une publication ({@code V006}, {@code V008}). Les invariants vivent dans
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

    /**
     * Identifiant d'une catégorie du module {@code taxonomy} : jamais une entité de ce module (D-AN).
     */
    @Column(name = "category_id")
    private Long categoryId;

    /**
     * Identifiants des tags ({@code publication_tag}). {@code @BatchSize} : pour une page, une seule
     * requête charge les tags de toutes les publications chargées (D-AS).
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "publication_tag", joinColumns = @JoinColumn(name = "publication_id"))
    @Column(name = "tag_id", nullable = false)
    @BatchSize(size = 100)
    private Set<Long> tagIds = new HashSet<>();

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
                              boolean featured, Long categoryId, Set<Long> tagIds,
                              String seoTitle, String seoDescription,
                              Instant createdAt, Instant updatedAt) {
        this.type = type;
        this.title = title;
        this.slug = slug;
        this.summary = summary;
        this.contentMarkdown = contentMarkdown;
        this.status = status;
        this.publishedAt = publishedAt;
        this.featured = featured;
        this.categoryId = categoryId;
        this.tagIds = new HashSet<>(tagIds);
        this.seoTitle = seoTitle;
        this.seoDescription = seoDescription;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
