package com.scalke.portfolio.backend.series.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Length;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

/**
 * Structure de persistance d'une série ({@code V011}). Les invariants vivent dans
 * {@code domain.model.Series} et dans les contraintes des tables (ADR 0001).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "series")
public class SeriesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "slug", nullable = false, length = 160)
    private String slug;

    @Column(name = "description_markdown", nullable = false, length = Length.LONG32, columnDefinition = "TEXT")
    private String descriptionMarkdown;

    /**
     * Articles de la série ; l'ordre est fixé par le domaine ({@code Series}). {@code @BatchSize} : pour une
     * page de séries, une seule requête charge les articles de toutes les séries chargées (D-BJ).
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "series_item", joinColumns = @JoinColumn(name = "series_id"))
    @BatchSize(size = 100)
    private List<SeriesItemEmbeddable> items = new ArrayList<>();

    @Builder
    private SeriesEntity(String title, String slug, String descriptionMarkdown, List<SeriesItemEmbeddable> items) {
        this.title = title;
        this.slug = slug;
        this.descriptionMarkdown = descriptionMarkdown;
        this.items = new ArrayList<>(items);
    }
}
