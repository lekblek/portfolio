package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Certification du parcours. Créée par son builder, puis rattachée au profil par
 * {@link ProfileEntity#addCertification(CertificationEntity)}.
 * <p>
 * Structure de persistance : l'invariant sur les dates vit dans le modèle métier
 * ({@code domain.model.Certification}) et dans la contrainte {@code certification_dates_check}.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "certification")
public class CertificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProfileEntity profile;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "issuer", nullable = false, length = 120)
    private String issuer;

    @Column(name = "issued_at", nullable = false)
    private LocalDate issuedAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "credential_url", length = 2048)
    private String credentialUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Builder
    private CertificationEntity(String name, String issuer, LocalDate issuedAt, LocalDate expiresAt,
                                String credentialUrl, int displayOrder) {
        this.name = name;
        this.issuer = issuer;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.credentialUrl = credentialUrl;
        this.displayOrder = displayOrder;
    }

    void attachTo(ProfileEntity profile) {
        this.profile = profile;
    }
}
