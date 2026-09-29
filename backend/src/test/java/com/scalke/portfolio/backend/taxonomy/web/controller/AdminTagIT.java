package com.scalke.portfolio.backend.taxonomy.web.controller;

import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import com.scalke.portfolio.backend.taxonomy.domain.port.TagRepository;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static com.scalke.portfolio.backend.publication.PublicationFixtures.classifiedArticle;
import static com.scalke.portfolio.backend.testsupport.CsrfTestSupport.xsrf;
import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Administration des tags de bout en bout (D-CS). Les règles communes aux termes sont détaillées par
 * {@link AdminCategoryIT} ; ici, ce qui est propre aux tags.
 */
@Transactional
class AdminTagIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    PublicationRepository publicationRepository;

    @Test
    void creates_lists_updates_and_deletes_a_tag() throws Exception {
        create("{\"name\":\"Spring Boot\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value("spring-boot"))
            .andExpect(jsonPath("$.description").doesNotExist());
        long id = idOf("spring-boot");

        mockMvc.perform(put("/api/admin/tags/" + id).contextPath("/api").with(user("admin")).with(xsrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Spring\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Spring"))
            .andExpect(jsonPath("$.slug").value("spring-boot"));
        mockMvc.perform(get("/api/admin/tags").contextPath("/api").with(user("admin")))
            .andExpect(jsonPath("$[0].name").value("Spring"));
        mockMvc.perform(delete("/api/admin/tags/" + id).contextPath("/api").with(user("admin")).with(xsrf()))
            .andExpect(status().isNoContent());
    }

    /**
     * D-CS : la colonne du slug d'un tag est plus courte (60) que ce que la génération produirait (« œ » → « oe »).
     */
    @Test
    void generates_a_slug_that_fits_its_column() throws Exception {
        create("{\"name\":\"" + "œ".repeat(60) + "\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value("oe".repeat(30)));
    }

    @Test
    void refuses_to_delete_a_tag_in_use() throws Exception {
        create("{\"name\":\"Java\"}");
        publicationRepository.create(classifiedArticle("article", NOW, null, idOf("java")));

        mockMvc.perform(delete("/api/admin/tags/" + idOf("java")).contextPath("/api").with(user("admin")).with(xsrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("TERM_STILL_USED"));
    }

    @Test
    void translates_a_name_uniqueness_violation_that_slipped_past_the_checks() {
        tagRepository.create(new Tag(null, "Java", Slug.of("java")));

        assertThatThrownBy(() -> tagRepository.create(new Tag(null, "JAVA", Slug.of("java-2"))))
            .isInstanceOfSatisfying(BusinessRuleViolationException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.NAME_ALREADY_USED));
    }

    @Test
    void translates_a_slug_taken_meanwhile() {
        tagRepository.create(new Tag(null, "Java", Slug.of("java")));

        assertThatThrownBy(() -> tagRepository.create(new Tag(null, "Autre", Slug.of("java"))))
            .isInstanceOfSatisfying(BusinessRuleViolationException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.SLUG_ALREADY_USED));
    }

    private ResultActions create(String json) throws Exception {
        return mockMvc.perform(post("/api/admin/tags").contextPath("/api").with(user("admin")).with(xsrf())
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long idOf(String slug) {
        return tagRepository.findBySlug(Slug.of(slug)).orElseThrow().id();
    }
}
