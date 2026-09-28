package com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Ligne de {@code project_screenshot} : composant du projet, sans identité propre ({@code 02} §12).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Embeddable
public class ProjectScreenshotEmbeddable {

    /**
     * Identifiant d'un média du module {@code media} : jamais une entité de ce module (ADR 0002).
     */
    @Column(name = "media_id", nullable = false)
    private Long mediaId;

    @Column(name = "caption", length = 300)
    private String caption;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
