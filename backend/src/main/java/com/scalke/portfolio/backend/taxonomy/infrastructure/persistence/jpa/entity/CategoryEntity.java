package com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Structure de persistance d'une catégorie ({@code V007}). Unicités et format du slug garantis par la table.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "category")
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "slug", nullable = false, length = 80)
    private String slug;

    @Column(name = "description", length = 500)
    private String description;

    @Builder
    private CategoryEntity(String name, String slug, String description) {
        this.name = name;
        this.slug = slug;
        this.description = description;
    }
}
