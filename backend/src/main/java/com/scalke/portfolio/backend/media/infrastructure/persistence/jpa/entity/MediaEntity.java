package com.scalke.portfolio.backend.media.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Structure de persistance d'un média ({@code V012}). Les invariants vivent dans {@code domain.model.Media}
 * et dans les contraintes de la table (ADR 0001).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "media")
public class MediaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "storage_key", nullable = false, length = 40)
    private String storageKey;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "alt_text", length = 300)
    private String altText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Builder
    private MediaEntity(String storageKey, String originalName, long sizeBytes, Integer width, Integer height,
                        String altText, Instant createdAt) {
        this.storageKey = storageKey;
        this.originalName = originalName;
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
        this.altText = altText;
        this.createdAt = createdAt;
    }
}
