package com.scalke.portfolio.backend.profile.domain.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Certification du parcours. {@code expiresAt == null} : sans expiration (et non « en cours ») ;
 * c'est pourquoi elle ne réutilise pas
 * {@link com.scalke.portfolio.backend.shared.domain.model.DateRange} (D-M).
 * <p>
 * Invariant (D-I, D-N) : l'expiration n'est jamais antérieure à la délivrance. Doublé par
 * {@code certification_dates_check} de {@code V003}.
 */
public record Certification(
    Long id,
    String name,
    String issuer,
    LocalDate issuedAt,
    LocalDate expiresAt,
    String credentialUrl,
    int displayOrder
) {

    public Certification {
        Objects.requireNonNull(issuedAt, "issuedAt");
        if (expiresAt != null && expiresAt.isBefore(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must not be before issuedAt");
        }
    }
}
