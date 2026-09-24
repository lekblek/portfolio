package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CertificationEntityTest {

    @Test
    void rejects_a_certification_expiring_before_it_is_issued() {
        assertThatThrownBy(() -> CertificationEntity.builder()
            .name("Certification")
            .issuer("Émetteur")
            .issuedAt(LocalDate.of(2025, 3, 1))
            .expiresAt(LocalDate.of(2025, 1, 1))
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void is_attached_to_the_profile_it_is_added_to() {
        ProfileEntity profile = new ProfileEntity("Blek", "Développeur full-stack", "Bio courte");
        CertificationEntity certification = CertificationEntity.builder()
            .name("Certification")
            .issuer("Émetteur")
            .issuedAt(LocalDate.of(2025, 1, 1))
            .build();

        profile.addCertification(certification);

        assertThat(certification.getProfile()).isSameAs(profile);
        assertThat(profile.getCertifications()).containsExactly(certification);
    }
}
