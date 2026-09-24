package com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Structure de persistance d'un tag ({@code V007}). Unicités et format du slug garantis par la table.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tag")
public class TagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "slug", nullable = false, length = 60)
    private String slug;

    @Builder
    private TagEntity(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }
}
