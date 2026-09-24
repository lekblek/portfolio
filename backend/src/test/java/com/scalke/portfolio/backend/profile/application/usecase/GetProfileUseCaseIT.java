package com.scalke.portfolio.backend.profile.application.usecase;

import com.scalke.portfolio.backend.profile.domain.model.ProfessionalLink;
import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.model.Skill;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfileEntity;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class GetProfileUseCaseIT extends AbstractIntegrationTest {

    @Autowired
    GetProfileUseCase getProfileUseCase;

    @Autowired
    EntityManager entityManager;

    @Test
    void fails_when_no_profile_exists() {
        assertThatThrownBy(() -> getProfileUseCase.execute())
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void returns_the_profile_with_ordered_links_and_skills() {
        givenACompleteProfile();

        Profile profile = getProfileUseCase.execute();

        assertThat(profile.links())
            .extracting(ProfessionalLink::label)
            .containsExactly("GitHub", "LinkedIn");

        assertThat(profile.skills())
            .extracting(Skill::name)
            .containsExactly("Angular", "Spring Boot");
    }

    private void givenACompleteProfile() {
        ProfileEntity profile = new ProfileEntity(
            "Blek Gedeon Ngossanga",
            "Développeur full-stack",
            "Je conçois des solutions modernes et évolutives.");

        profile.addLink("LinkedIn", "https://example.test/in", 1);
        profile.addLink("GitHub", "https://example.test/gh", 0);
        profile.addSkill("Spring Boot", "Backend", 1);
        profile.addSkill("Angular", "Frontend", 0);

        entityManager.persist(profile);
        entityManager.flush();
        entityManager.clear();
    }
}
