package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.profile.domain.model.Certification;
import com.scalke.portfolio.backend.profile.domain.model.DateRange;
import com.scalke.portfolio.backend.profile.domain.model.Education;
import com.scalke.portfolio.backend.profile.domain.model.Experience;
import com.scalke.portfolio.backend.profile.domain.model.ProfessionalLink;
import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.model.Skill;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfileEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfileEntity.SINGLETON_ID;
import static org.assertj.core.api.Assertions.assertThat;

class ProfilePersistenceMapperTest {

    @Test
    void keeps_every_field_through_a_round_trip() {
        Profile profile = new Profile(
            "Blek Gedeon Ngossanga",
            "Développeur full-stack",
            "Bio courte",
            "# À propos",
            "Tanger",
            "contact@example.test",
            List.of(new ProfessionalLink(null, "GitHub", "https://example.test/gh", 0)),
            List.of(new Skill(null, "Java", "Backend", 0)),
            List.of(new Experience(null, "Scalke", "Fondateur", "Tanger",
                DateRange.ongoingSince(LocalDate.of(2024, 1, 1)), "Plateforme SaaS", 0)),
            List.of(new Education(null, "ENSA Tanger", "Ingénieur", "Informatique", "Tanger",
                DateRange.between(LocalDate.of(2018, 9, 1), LocalDate.of(2023, 6, 30)), "Cycle ingénieur", 1)),
            List.of(new Certification(null, "Certification", "Émetteur",
                LocalDate.of(2025, 1, 1), LocalDate.of(2027, 1, 1), "https://example.test/cert", 2)));

        ProfileEntity entity = ProfilePersistenceMapper.toEntity(profile);

        assertThat(entity.getId()).isEqualTo(SINGLETON_ID);
        assertThat(entity.getLinks()).allSatisfy(link -> assertThat(link.getProfile()).isSameAs(entity));
        assertThat(entity.getSkills()).allSatisfy(skill -> assertThat(skill.getProfile()).isSameAs(entity));
        assertThat(entity.getExperiences()).allSatisfy(e -> assertThat(e.getProfile()).isSameAs(entity));
        assertThat(entity.getEducations()).allSatisfy(e -> assertThat(e.getProfile()).isSameAs(entity));
        assertThat(entity.getCertifications()).allSatisfy(c -> assertThat(c.getProfile()).isSameAs(entity));
        assertThat(ProfilePersistenceMapper.toDomain(entity)).isEqualTo(profile);
    }
}
