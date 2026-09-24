package com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.entity;

import com.scalke.portfolio.backend.project.domain.model.ProjectStage;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Length;

import java.time.LocalDate;

/**
 * Structure de persistance d'un projet ({@code V004}). Les invariants vivent dans
 * {@code domain.model.Project} et dans les contraintes de la table (ADR 0001).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "project")
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "slug", nullable = false, length = 160)
    private String slug;

    @Column(name = "short_description", nullable = false, length = 500)
    private String shortDescription;

    @Column(name = "description_markdown", nullable = false, length = Length.LONG32, columnDefinition = "TEXT")
    private String descriptionMarkdown;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 20)
    private ProjectStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private ProjectVisibility visibility;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "repository_url", length = 2048)
    private String repositoryUrl;

    @Column(name = "demo_url", length = 2048)
    private String demoUrl;

    @Column(name = "featured", nullable = false)
    private boolean featured;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Builder
    private ProjectEntity(String title, String slug, String shortDescription, String descriptionMarkdown,
                          ProjectStage stage, ProjectVisibility visibility, LocalDate startDate, LocalDate endDate,
                          String repositoryUrl, String demoUrl, boolean featured, int displayOrder) {
        this.title = title;
        this.slug = slug;
        this.shortDescription = shortDescription;
        this.descriptionMarkdown = descriptionMarkdown;
        this.stage = stage;
        this.visibility = visibility;
        this.startDate = startDate;
        this.endDate = endDate;
        this.repositoryUrl = repositoryUrl;
        this.demoUrl = demoUrl;
        this.featured = featured;
        this.displayOrder = displayOrder;
    }
}
