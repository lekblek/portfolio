package com.scalke.portfolio.backend.profile;

import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ProfileEntity;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
}
