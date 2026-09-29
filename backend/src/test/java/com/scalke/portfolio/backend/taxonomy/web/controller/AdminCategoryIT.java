package com.scalke.portfolio.backend.taxonomy.web.controller;

import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.port.CategoryRepository;
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
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Administration des catégories de bout en bout (D-CS) : session, CSRF, validation, slug généré ou suffixé, conflits
 * de nom, catégorie utilisée.
 */
@Transactional
class AdminCategoryIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    PublicationRepository publicationRepository;

    @Test
    void requires_the_administrator_session() throws Exception {
        mockMvc.perform(get("/api/admin/categories").contextPath("/api"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void refuses_a_write_without_csrf_token() throws Exception {
        mockMvc.perform(post("/api/admin/categories").contextPath("/api").with(user("admin"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Backend\"}"))
            .andExpect(status().isForbidden());
    }

    /**
     * Slug généré depuis le nom ; description vide enregistrée comme absente ; 201 avec l'adresse de la ressource.
     */
    @Test
    void creates_a_category_with_a_generated_slug() throws Exception {
        create("{\"name\":\" Architecture logicielle \",\"description\":\"  \"}")
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", endsWith("/api/admin/categories/" + idOf("architecture-logicielle"))))
            .andExpect(jsonPath("$.name").value("Architecture logicielle"))
            .andExpect(jsonPath("$.slug").value("architecture-logicielle"))
            .andExpect(jsonPath("$.description").value(nullValue()));
    }

    /**
     * D-BD : un slug déjà pris reçoit le premier suffixe libre.
     */
    @Test
    void suffixes_a_slug_already_taken() throws Exception {
        create("{\"name\":\"Backend\"}").andExpect(status().isCreated());

        create("{\"name\":\"Serveur\",\"slug\":\"backend\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value("backend-2"));
    }

    /**
     * Invariant 25 : le nom est unique sans tenir compte de la casse.
     */
    @Test
    void refuses_a_name_that_differs_only_by_case() throws Exception {
        create("{\"name\":\"Backend\"}").andExpect(status().isCreated());

        create("{\"name\":\"BACKEND\"}")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("NAME_ALREADY_USED"));
    }

    @Test
    void validates_the_name_and_the_slug() throws Exception {
        create("{\"name\":\"!!!\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("name"));
        create("{\"name\":\"Backend\",\"slug\":\"Mauvais Slug\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("slug"));
    }

    @Test
    void lists_the_categories_by_name_ignoring_case() throws Exception {
        create("{\"name\":\"frontend\"}");
        create("{\"name\":\"Backend\"}");

        mockMvc.perform(get("/api/admin/categories").contextPath("/api").with(user("admin")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Backend"))
            .andExpect(jsonPath("$[0].id").isNumber())
            .andExpect(jsonPath("$[1].name").value("frontend"));
    }

    /**
     * Sans slug saisi, la modification garde le slug ; avec un slug saisi, elle le remplace (un terme n'est pas publié).
     */
    @Test
    void keeps_or_replaces_the_slug_when_updating() throws Exception {
        create("{\"name\":\"Backend\"}");
        long id = idOf("backend");

        update(id, "{\"name\":\"Back-end\",\"description\":\"API et persistance\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Back-end"))
            .andExpect(jsonPath("$.slug").value("backend"))
            .andExpect(jsonPath("$.description").value("API et persistance"));
        update(id, "{\"name\":\"Back-end\",\"slug\":\"serveur\"}")
            .andExpect(jsonPath("$.slug").value("serveur"));
    }

    @Test
    void an_unknown_category_is_not_found() throws Exception {
        update(999_999L, "{\"name\":\"Backend\"}").andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/admin/categories/999999").contextPath("/api").with(user("admin")).with(xsrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    void deletes_an_unused_category() throws Exception {
        create("{\"name\":\"Backend\"}");

        mockMvc.perform(delete("/api/admin/categories/" + idOf("backend")).contextPath("/api")
                .with(user("admin")).with(xsrf()))
            .andExpect(status().isNoContent());
        assertThat(categoryRepository.findBySlug(Slug.of("backend"))).isEmpty();
    }

    /**
     * Invariant 25 : une catégorie utilisée par une publication ne peut pas être supprimée.
     */
    @Test
    void refuses_to_delete_a_category_in_use() throws Exception {
        create("{\"name\":\"Backend\"}");
        publicationRepository.create(classifiedArticle("article", NOW, idOf("backend")));

        mockMvc.perform(delete("/api/admin/categories/" + idOf("backend")).contextPath("/api")
                .with(user("admin")).with(xsrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("TERM_STILL_USED"));
    }

    /**
     * Écriture concurrente : la contrainte d'unicité de PostgreSQL est traduite en erreur métier, pas en 500.
     */
    @Test
    void translates_a_uniqueness_violation_that_slipped_past_the_checks() {
        categoryRepository.create(new Category(null, "Backend", Slug.of("backend"), null));

        assertThatThrownBy(() -> categoryRepository.create(new Category(null, "Autre", Slug.of("backend"), null)))
            .isInstanceOfSatisfying(BusinessRuleViolationException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.SLUG_ALREADY_USED));
    }

    private ResultActions create(String json) throws Exception {
        return mockMvc.perform(post("/api/admin/categories").contextPath("/api").with(user("admin")).with(xsrf())
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions update(long id, String json) throws Exception {
        return mockMvc.perform(put("/api/admin/categories/" + id).contextPath("/api").with(user("admin")).with(xsrf())
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long idOf(String slug) {
        return categoryRepository.findBySlug(Slug.of(slug)).orElseThrow().id();
    }
}
