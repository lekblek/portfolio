package com.scalke.portfolio.backend.profile;

import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.CertificationEntity;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ExperienceEntity;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfileEntity;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parcours HTTP complet : contrôleur → cas d'usage → adaptateur JPA → PostgreSQL.
 */
@Transactional
class PublicProfileIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    EntityManager entityManager;

    @Test
    void returns_404_as_problem_detail_when_no_profile_exists() throws Exception {
        mockMvc.perform(get("/api/public/profile").contextPath("/api"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void returns_the_stored_profile_with_ordered_links_and_grouped_skills() throws Exception {
        ProfileEntity profile = new ProfileEntity("Blek Gedeon Ngossanga", "Développeur full-stack", "Bio courte");
        profile.setPublicEmail("contact@example.test");
        profile.addLink("LinkedIn", "https://example.test/in", 1);
        profile.addLink("GitHub", "https://example.test/gh", 0);
        profile.addSkill("PostgreSQL", "Backend", 1);
        profile.addSkill("Spring Boot", "Backend", 0);
        profile.addSkill("Angular", "Frontend", 2);
        entityManager.persist(profile);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/public/profile").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Blek Gedeon Ngossanga"))
            .andExpect(jsonPath("$.publicEmail").value("contact@example.test"))
            .andExpect(jsonPath("$.links[0].label").value("GitHub"))
            .andExpect(jsonPath("$.links[1].label").value("LinkedIn"))
            .andExpect(jsonPath("$.skillGroups[0].category").value("Backend"))
            .andExpect(jsonPath("$.skillGroups[0].skills[0].name").value("Spring Boot"))
            .andExpect(jsonPath("$.skillGroups[0].skills[1].name").value("PostgreSQL"))
            .andExpect(jsonPath("$.skillGroups[1].category").value("Frontend"));
    }

    /**
     * Contrat public du parcours (C09, C10, D-P, D-R), vérifié sur la sérialisation réelle.
     */
    @Test
    void exposes_career_entries_with_iso_dates_explicit_nulls_and_no_technical_fields() throws Exception {
        ProfileEntity profile = new ProfileEntity("Blek Gedeon Ngossanga", "Développeur full-stack", "Bio courte");
        profile.addLink("GitHub", "https://example.test/gh", 0);
        profile.addSkill("Java", "Backend", 0);
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
        profile.addCertification(CertificationEntity.builder()
            .name("Certification").issuer("Émetteur")
            .issuedAt(LocalDate.of(2025, 1, 1))
            .displayOrder(0)
            .build());
        entityManager.persist(profile);
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/public/profile").contextPath("/api"))
            .andExpect(status().isOk())
            // tri D-L : même displayOrder, la plus récente d'abord
            .andExpect(jsonPath("$.experiences[0].organization").value("Scalke"))
            .andExpect(jsonPath("$.experiences[1].organization").value("Org B"))
            // dates ISO-8601 (C09) et null explicite pour « en cours » (C10, D-P)
            .andExpect(jsonPath("$.experiences[0].startDate").value("2024-01-01"))
            .andExpect(jsonPath("$.experiences[0].endDate").hasJsonPath())
            .andExpect(jsonPath("$.experiences[0].endDate").value(nullValue()))
            .andExpect(jsonPath("$.experiences[1].endDate").value("2023-12-31"))
            .andExpect(jsonPath("$.experiences[0].period").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.certifications[0].issuedAt").value("2025-01-01"))
            .andExpect(jsonPath("$.certifications[0].expiresAt").hasJsonPath())
            .andExpect(jsonPath("$.certifications[0].expiresAt").value(nullValue()))
            .andExpect(jsonPath("$.certifications[0].credentialUrl").hasJsonPath())
            // collection vide : [] et jamais null
            .andExpect(jsonPath("$.educations").isArray())
            .andExpect(jsonPath("$.educations").isEmpty())
            // aucun champ technique public (C03, D-R)
            .andExpect(jsonPath("$.links[0].displayOrder").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.skillGroups[0].skills[0].id").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.skillGroups[0].skills[0].displayOrder").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.experiences[0].id").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.experiences[0].displayOrder").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.certifications[0].id").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.certifications[0].displayOrder").doesNotHaveJsonPath());
    }
}
