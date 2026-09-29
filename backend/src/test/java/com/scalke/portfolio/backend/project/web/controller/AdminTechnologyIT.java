package com.scalke.portfolio.backend.project.web.controller;

import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.project.domain.port.TechnologyRepository;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.scalke.portfolio.backend.project.ProjectFixtures.published;
import static com.scalke.portfolio.backend.project.ProjectFixtures.technology;
import static com.scalke.portfolio.backend.testsupport.CsrfTestSupport.xsrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Administration du vocabulaire des technologies de bout en bout (D-CW) : les règles communes aux termes sont
 * détaillées par {@code AdminCategoryIT} ; ici, ce qui est propre aux technologies.
 */
@Transactional
class AdminTechnologyIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TechnologyRepository technologyRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    void requires_the_administrator_session() throws Exception {
        mockMvc.perform(get("/api/admin/technologies").contextPath("/api"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void creates_lists_updates_and_deletes_a_technology() throws Exception {
        create("{\"name\":\"Spring Boot\",\"displayOrder\":2}")
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", endsWith("/api/admin/technologies/" + idOf("spring-boot"))))
            .andExpect(jsonPath("$.slug").value("spring-boot"))
            .andExpect(jsonPath("$.displayOrder").value(2));
        create("{\"name\":\"Java\"}").andExpect(jsonPath("$.displayOrder").value(0));
        long id = idOf("spring-boot");

        mockMvc.perform(get("/api/admin/technologies").contextPath("/api").with(user("admin")))
            .andExpect(jsonPath("$[0].name").value("Java"))
            .andExpect(jsonPath("$[1].name").value("Spring Boot"));
        mockMvc.perform(put("/api/admin/technologies/" + id).contextPath("/api").with(user("admin")).with(xsrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Spring\",\"slug\":\"spring\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value("spring"))
            .andExpect(jsonPath("$.displayOrder").value(0));
        mockMvc.perform(delete("/api/admin/technologies/" + id).contextPath("/api").with(user("admin")).with(xsrf()))
            .andExpect(status().isNoContent());
        assertThat(technologyRepository.findById(id)).isEmpty();
    }

    /**
     * Invariant 22 : nom unique sans tenir compte de la casse ; slug suffixé dans la longueur de sa colonne (80).
     */
    @Test
    void keeps_the_vocabulary_unique() throws Exception {
        create("{\"name\":\"Java\"}");

        create("{\"name\":\"JAVA\"}")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("NAME_ALREADY_USED"));
        create("{\"name\":\"Langage\",\"slug\":\"java\"}").andExpect(jsonPath("$.slug").value("java-2"));
        create("{\"name\":\"" + "œ".repeat(80) + "\"}").andExpect(jsonPath("$.slug").value("oe".repeat(40)));
    }

    @Test
    void validates_the_technology() throws Exception {
        create("{\"name\":\"Java\",\"displayOrder\":-1}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("displayOrder"));
        create("{\"name\":\"" + "a".repeat(81) + "\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    /**
     * Invariant 22 : une technologie utilisée par un projet n'est pas supprimée.
     */
    @Test
    void refuses_to_delete_a_technology_in_use() throws Exception {
        Technology java = technologyRepository.create(technology("Java", "java", 0));
        projectRepository.create(published("portfolio", LocalDate.of(2026, 1, 1), 0, java));
        // Une vraie requête part d'un contexte de persistance vide : le projet n'y est pas chargé.
        entityManager.clear();

        mockMvc.perform(delete("/api/admin/technologies/" + java.id()).contextPath("/api").with(user("admin"))
                .with(xsrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("TERM_STILL_USED"));
    }

    @Test
    void translates_a_name_taken_meanwhile() {
        technologyRepository.create(technology("Java", "java", 0));

        assertThatThrownBy(() -> technologyRepository.create(technology("JAVA", "java-2", 0)))
            .isInstanceOfSatisfying(BusinessRuleViolationException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.NAME_ALREADY_USED));
    }

    @Test
    void translates_a_slug_taken_meanwhile() {
        technologyRepository.create(technology("Java", "java", 0));

        assertThatThrownBy(() -> technologyRepository.create(technology("Autre", "java", 1)))
            .isInstanceOfSatisfying(BusinessRuleViolationException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.SLUG_ALREADY_USED));
    }

    private ResultActions create(String json) throws Exception {
        return mockMvc.perform(post("/api/admin/technologies").contextPath("/api").with(user("admin")).with(xsrf())
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long idOf(String slug) {
        return technologyRepository.findAll().stream()
            .filter(technology -> technology.slug().value().equals(slug))
            .findFirst().orElseThrow().id();
    }
}
