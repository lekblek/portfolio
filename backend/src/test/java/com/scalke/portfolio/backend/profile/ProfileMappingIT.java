package com.scalke.portfolio.backend.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfileEntity;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ProfileMappingIT extends AbstractIntegrationTest {

    @Autowired
    EntityManager entityManager;

    @Test
    void persists_a_profile_with_its_links() {
        ProfileEntity profile = new ProfileEntity(
            "Blek Gedeon Ngossanga",
            "Développeur full-stack",
            "Je conçois des solutions modernes et évolutives.");

        profile.addLink("LinkedIn", "https://example.test/in", 1);
        profile.addLink("GitHub", "https://example.test/gh", 0);

        entityManager.persist(profile);
        entityManager.flush();
        entityManager.clear();

        ProfileEntity reloaded = entityManager.find(ProfileEntity.class, profile.getId());

        assertThat(reloaded.getId()).isEqualTo(1L);
        assertThat(reloaded.getDisplayName()).isEqualTo("Blek Gedeon Ngossanga");
        assertThat(reloaded.getLinks())
            .extracting(link -> link.getLabel())
            .containsExactly("GitHub", "LinkedIn");
    }
}
