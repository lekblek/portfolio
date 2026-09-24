package com.scalke.portfolio.backend.profile.web.dto;

import com.scalke.portfolio.backend.profile.domain.model.Certification;

import java.time.LocalDate;

/**
 * Certification publique. {@code expiresAt == null} : sans expiration.
 */
public record CertificationResponse(
    String name,
    String issuer,
    LocalDate issuedAt,
    LocalDate expiresAt,
    String credentialUrl
) {

    static CertificationResponse from(Certification certification) {
        return new CertificationResponse(
            certification.name(),
            certification.issuer(),
            certification.issuedAt(),
            certification.expiresAt(),
            certification.credentialUrl());
    }
}
