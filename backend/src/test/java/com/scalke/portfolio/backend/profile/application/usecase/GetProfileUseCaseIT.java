package com.scalke.portfolio.backend.profile.application.usecase;

import com.scalke.portfolio.backend.profile.domain.model.Certification;
import com.scalke.portfolio.backend.profile.domain.model.Education;
import com.scalke.portfolio.backend.profile.domain.model.Experience;
import com.scalke.portfolio.backend.profile.domain.model.ProfessionalLink;
import com.scalke.portfolio.backend.profile.domain.model.Profile;
import com.scalke.portfolio.backend.profile.domain.model.Skill;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.CertificationEntity;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.EducationEntity;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ExperienceEntity;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfileEntity;
import com.scalke.portfolio.backend.shared.error.ResourceNotFoundException;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class GetProfileUseCaseIT extends AbstractIntegrationTest {

    @Autowired
    GetProfileUseCase getProfileUseCase;

    @Autowired
    EntityManager entityManager;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Test
    void fails_when_no_profile_exists() {
        assertThatThrownBy(() -> getProfileUseCase.execute())
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void returns_the_profile_with_its_five_ordered_collections() {
        givenACompleteProfile();

        Profile profile = getProfileUseCase.execute();

        assertThat(profile.links())
            .extracting(ProfessionalLink::label)
            .containsExactly("GitHub", "LinkedIn");
        assertThat(profile.skills())
            .extracting(Skill::name)
            .containsExactly("Angular", "Spring Boot");
        assertThat(profile.experiences())
            .extracting(Experience::organization)
            .containsExactly("Scalke", "Org B");
        assertThat(profile.experiences().getFirst().period().isOngoing()).isTrue();
        assertThat(profile.educations())
            .extracting(Education::institution)
            .containsExactly("ENSA Tanger");
        assertThat(profile.certifications())
            .singleElement()
            .satisfies(certification -> {
                assertThat(certification.expiresAt()).isNull();
                assertThat(certification.credentialUrl()).isNull();
            });
    }

    /**
     * D-O : le coût de lecture dépend de la structure de l'agrégat (1 racine + 5 collections),
     * pas du volume de données. Un chargement élément par élément (N+1) ferait croître ce nombre.
     */
    @Test
    void loads_the_profile_with_one_query_for_the_root_and_one_per_collection() {
        givenACompleteProfile();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        getProfileUseCase.execute();

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1 + 5);
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
        profile.addExperience(ExperienceEntity.builder()
            .organization("Org B").title("Développeur").location("Tanger")
            .startDate(LocalDate.of(2021, 9, 1)).endDate(LocalDate.of(2023, 12, 31))
            .description("Backend Java").displayOrder(0)
            .build());
        profile.addExperience(ExperienceEntity.builder()
            .organization("Scalke").title("Fondateur").location("Tanger")
            .startDate(LocalDate.of(2024, 1, 1))
            .description("Plateforme SaaS").displayOrder(0)
            .build());
        profile.addEducation(EducationEntity.builder()
            .institution("ENSA Tanger").degree("Doctorat").field("Informatique").location("Tanger")
            .startDate(LocalDate.of(2023, 12, 1))
            .description("Thèse").displayOrder(0)
            .build());
        profile.addCertification(CertificationEntity.builder()
            .name("Certification").issuer("Émetteur")
            .issuedAt(LocalDate.of(2025, 1, 1))
            .displayOrder(0)
            .build());

        entityManager.persist(profile);
        entityManager.flush();
        entityManager.clear();
    }
}
