package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CertificationEntityTest {

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
