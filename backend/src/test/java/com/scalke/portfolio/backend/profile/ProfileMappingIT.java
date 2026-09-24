package com.scalke.portfolio.backend.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.*;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Transactional
class ProfileMappingIT extends AbstractIntegrationTest {

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
            .extracting(ProfessionalLinkEntity::getLabel)
            .containsExactly("GitHub", "LinkedIn");
    }

    @Test
    void persists_a_profile_with_its_skills() {
        ProfileEntity profile = new ProfileEntity("Blek", "Développeur full-stack", "Bio courte");
        profile.addSkill("Spring Boot", "Backend", 1);
        profile.addSkill("Angular", "Frontend", 0);

        entityManager.persist(profile);
        entityManager.flush();
        entityManager.clear();

        ProfileEntity reloaded = entityManager.find(ProfileEntity.class, profile.getId());

        assertThat(reloaded.getSkills())
            .extracting(SkillEntity::getName)
            .containsExactly("Angular", "Spring Boot");
    }

    @Test
    void orders_experiences_by_display_order_then_most_recent_first() {
        ProfileEntity profile = new ProfileEntity("Blek Gedeon Ngossanga", "Développeur full-stack", "Bio courte");

        profile.addExperience(
            ExperienceEntity.builder()
                .organization("Org A")
                .title("Poste A")
                .location("Tanger")
                .period(DateRange.between(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1)))
                .description("...")
                .displayOrder(1)
                .build()
        );

        profile.addExperience(
            ExperienceEntity.builder()
                .organization("Org B")
                .title("Poste B")
                .location("Tanger")
                .period(DateRange.between(
                    LocalDate.of(2019, 1, 1), LocalDate.of(2020, 1, 1)))
                .description("...")
                .displayOrder(0)
                .build()
        );

        profile.addExperience(
            ExperienceEntity.builder()
                .organization("Org C")
                .title("Poste C")
                .location("Tanger")
                .period(DateRange.ongoingSince(
                    LocalDate.of(2023, 1, 1)))
                .description("...")
                .displayOrder(0)
                .build()
        );

        profile.addExperience(
            ExperienceEntity.builder()
                .organization("Org D")
                .title("Poste D")
                .location("Tanger")
                .period(DateRange.ongoingSince(LocalDate.of(2023, 1, 1)))
                .description("...")
                .displayOrder(0)
                .build()
        );

        ProfileEntity reloaded = persistAndReload(profile);

        assertThat(reloaded.getExperiences())
            .extracting(ExperienceEntity::getOrganization)
            .containsExactly("Org C", "Org D", "Org B", "Org A");
    }

    @Test
    void persists_education_and_certification_with_optional_values() {
        ProfileEntity profile = new ProfileEntity("Blek Gedeon Ngossanga", "Développeur full-stack", "Bio courte");

        profile.addEducation(
            EducationEntity.builder()
                .institution("ENSA Tanger")
                .degree("Doctorat")
                .field("Informatique")
                .location("Tanger")
                .period(DateRange.ongoingSince(LocalDate.of(2023, 12, 1)))
                .description("...")
                .displayOrder(0)
                .build()
        );

        profile.addCertification(
            CertificationEntity.builder()
                .name("Certification")
                .issuer("Émetteur")
                .issuedAt(LocalDate.of(2025, 1, 1))
                .displayOrder(0)
                .build()
        );

        ProfileEntity reloaded = persistAndReload(profile);

        assertThat(reloaded.getEducations()).singleElement()
            .satisfies(e -> assertThat(e.getPeriod().isOngoing()).isTrue());
        assertThat(reloaded.getCertifications()).singleElement()
            .satisfies(c -> {
                assertThat(c.getExpiresAt()).isNull();
                assertThat(c.getCredentialUrl()).isNull();
            });
    }

    private ProfileEntity persistAndReload(ProfileEntity profile) {
        entityManager.persist(profile);
        entityManager.flush();
        entityManager.clear();
        return entityManager.find(ProfileEntity.class, profile.getId());
    }
}
