package com.scalke.portfolio.backend.profile.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CertificationTest {

    @Test
    void rejects_a_certification_expiring_before_it_is_issued() {
        assertThatThrownBy(() -> certification(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 1, 1)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void accepts_a_certification_expiring_the_day_it_is_issued() {
        LocalDate day = LocalDate.of(2025, 3, 1);

        assertThat(certification(day, day).expiresAt()).isEqualTo(day);
    }

    @Test
    void accepts_a_certification_without_expiry() {
        assertThat(certification(LocalDate.of(2025, 3, 1), null).expiresAt()).isNull();
    }

    private static Certification certification(LocalDate issuedAt, LocalDate expiresAt) {
        return new Certification(null, "Certification", "Émetteur", issuedAt, expiresAt, null, 0);
    }
}
