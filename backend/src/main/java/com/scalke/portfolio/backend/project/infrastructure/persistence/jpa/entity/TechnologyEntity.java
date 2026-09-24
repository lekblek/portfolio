package com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Structure de persistance d'une technologie ({@code V005}). Les unicités et le format du slug sont
 * garantis par la table (D-AA).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "technology")
public class TechnologyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "slug", nullable = false, length = 80)
    private String slug;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Builder
    private TechnologyEntity(String name, String slug, int displayOrder) {
        this.name = name;
        this.slug = slug;
        this.displayOrder = displayOrder;
    }
}
