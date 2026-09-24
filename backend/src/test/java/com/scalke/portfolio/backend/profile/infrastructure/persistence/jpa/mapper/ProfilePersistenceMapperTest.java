package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.profile.domain.model.ProfessionalLink;
import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.model.Skill;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfileEntity;
import org.junit.jupiter.api.Test;

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
            List.of(new Skill(null, "Java", "Backend", 0)));

        ProfileEntity entity = ProfilePersistenceMapper.toEntity(profile);

        assertThat(entity.getId()).isEqualTo(SINGLETON_ID);
        assertThat(entity.getLinks()).allSatisfy(link -> assertThat(link.getProfile()).isSameAs(entity));
        assertThat(entity.getSkills()).allSatisfy(skill -> assertThat(skill.getProfile()).isSameAs(entity));
        assertThat(ProfilePersistenceMapper.toDomain(entity)).isEqualTo(profile);
    }
}
