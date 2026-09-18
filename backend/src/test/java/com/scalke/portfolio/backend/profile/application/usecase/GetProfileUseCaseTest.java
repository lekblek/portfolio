package com.scalke.portfolio.backend.profile.application.usecase;

import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfileEntity;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
class GetProfileUseCaseTest extends AbstractIntegrationTest {

    @Autowired
    GetProfileUseCase getProfileUseCase;

    @Autowired
    EntityManager entityManager;

    @Test
    void returns_the_profile_with_its_links_already_loaded() {
        givenAProfileWithTwoLinks();

        Profile profile = getProfileUseCase.execute();

        assertThat(profile.displayName()).isEqualTo("Blek Gedeon Ngossanga");

        assertThat(profile.links())
            .extracting(link -> link.label())
            .containsExactly("GitHub", "LinkedIn");
    }

    @Test
    void fails_when_no_profile_exists() {
        assertThatThrownBy(() -> getProfileUseCase.execute())
            .isInstanceOf(ResourceNotFoundException.class);
    }

    private void givenAProfileWithTwoLinks() {
        ProfileEntity profile = new ProfileEntity(
            "Blek Gedeon Ngossanga",
            "Développeur full-stack",
            "Je conçois des solutions modernes et évolutives.");

        profile.addLink("LinkedIn", "https://example.test/in", 1);
        profile.addLink("GitHub", "https://example.test/gh", 0);

        entityManager.persist(profile);
        entityManager.flush();
        entityManager.clear();
    }
}
