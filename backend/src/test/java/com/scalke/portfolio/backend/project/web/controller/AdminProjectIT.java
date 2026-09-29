package com.scalke.portfolio.backend.project.web.controller;

import com.scalke.portfolio.backend.media.MediaFixtures;
import com.scalke.portfolio.backend.media.domain.port.MediaRepository;
import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.project.domain.port.TechnologyRepository;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.scalke.portfolio.backend.project.ProjectFixtures.project;
import static com.scalke.portfolio.backend.project.ProjectFixtures.published;
import static com.scalke.portfolio.backend.project.ProjectFixtures.technology;
import static com.scalke.portfolio.backend.testsupport.CsrfTestSupport.xsrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Administration des projets de bout en bout (D-CX) : session, CSRF, validation (période, adresses, références),
 * technologies et captures remplacées, visibilité, slug verrouillé après publication, effet sur le site et la
 * recherche.
 */
@Transactional
class AdminProjectIT extends AbstractIntegrationTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    TechnologyRepository technologyRepository;

    @Autowired
    MediaRepository mediaRepository;

    @Test
    void requires_the_administrator_session_and_the_csrf_token() throws Exception {
        mockMvc.perform(get("/api/admin/projects").contextPath("/api"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/projects").contextPath("/api").with(user("admin"))
                .contentType(MediaType.APPLICATION_JSON).content(projectJson("Portfolio", "DRAFT", "")))
            .andExpect(status().isForbidden());
    }

    /**
     * Brouillon complet : technologies dans l'ordre du vocabulaire, captures dans l'ordre saisi, légende vide retirée ;
     * il n'apparaît pas sur le site.
     */
    @Test
    void creates_a_draft_project() throws Exception {
        Technology angular = technologyRepository.create(technology("Angular", "angular", 2));
        Technology java = technologyRepository.create(technology("Java", "java", 1));
        long cover = image();
        long first = image();
        long second = image();

        create(projectJson("Portfolio full-stack", "DRAFT", """
            ,"repositoryUrl":"https://github.com/exemple/portfolio","featured":true,"displayOrder":3,
            "technologyIds":[%d,%d],"coverMediaId":%d,
            "screenshots":[{"mediaId":%d,"caption":"Accueil"},{"mediaId":%d,"caption":" "}]
            """.formatted(angular.id(), java.id(), cover, second, first)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", endsWith("/api/admin/projects/" + idOf("portfolio-full-stack"))))
            .andExpect(jsonPath("$.slug").value("portfolio-full-stack"))
            .andExpect(jsonPath("$.slugLocked").value(false))
            .andExpect(jsonPath("$.visibility").value("DRAFT"))
            .andExpect(jsonPath("$.stage").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.endDate").value(nullValue()))
            .andExpect(jsonPath("$.featured").value(true))
            .andExpect(jsonPath("$.displayOrder").value(3))
            .andExpect(jsonPath("$.repositoryUrl").value("https://github.com/exemple/portfolio"))
            .andExpect(jsonPath("$.demoUrl").value(nullValue()))
            .andExpect(jsonPath("$.technologyIds[0]").value(java.id()))
            .andExpect(jsonPath("$.technologyIds[1]").value(angular.id()))
            .andExpect(jsonPath("$.coverMediaId").value(cover))
            .andExpect(jsonPath("$.screenshots[0].mediaId").value(second))
            .andExpect(jsonPath("$.screenshots[0].caption").value("Accueil"))
            .andExpect(jsonPath("$.screenshots[1].mediaId").value(first))
            .andExpect(jsonPath("$.screenshots[1].caption").value(nullValue()));

        mockMvc.perform(get("/api/public/projects/portfolio-full-stack").contextPath("/api"))
            .andExpect(status().isNotFound());
    }

    @Test
    void publishes_a_project_at_creation_and_suffixes_a_taken_slug() throws Exception {
        create(projectJson("Portfolio", "PUBLISHED", ""))
            .andExpect(jsonPath("$.slugLocked").value(true));
        create(projectJson("Portfolio", "DRAFT", "")).andExpect(jsonPath("$.slug").value("portfolio-2"));

        mockMvc.perform(get("/api/public/projects/portfolio").contextPath("/api"))
            .andExpect(status().isOk());
    }

    @Test
    void validates_the_request() throws Exception {
        create(projectJson("!!!", "DRAFT", ""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("title"));
        create(projectJson("Portfolio", "DRAFT", ",\"repositoryUrl\":\"javascript:alert(1)\""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("repositoryUrl"));
        create(projectJson("Portfolio", "DRAFT", ",\"displayOrder\":-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("displayOrder"));
        create(projectJson("Portfolio", "DRAFT", ",\"screenshots\":[{\"caption\":\"Sans image\"}]"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("screenshots[0].mediaId"));
        create("""
            {"title":"Portfolio","shortDescription":"Résumé","descriptionMarkdown":"","visibility":"DRAFT",
             "startDate":"2026-01-01"}
            """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("stage"));
    }

    /**
     * Invariants 17 et 21 : la période est vérifiée avant le domaine, sur le champ en cause.
     */
    @Test
    void validates_the_period() throws Exception {
        create(withPeriod("COMPLETED", "2026-01-01", "2025-12-31"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("endDate"));
        create(withPeriod("IN_PROGRESS", "2026-01-01", "2026-06-30"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("stage"));
        create(withPeriod("COMPLETED", "2026-01-01", null))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("stage"));
        create(withPeriod("COMPLETED", "2026-01-01", "2026-06-30"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.endDate").value("2026-06-30"));
    }

    /**
     * Une technologie inconnue, une couverture ou une capture qui n'est pas une image, une capture répétée : 400 sur le
     * champ (D-BV, invariant 27).
     */
    @Test
    void refuses_invalid_references() throws Exception {
        long pdf = mediaRepository.create(MediaFixtures.pdf()).id();
        long picture = image();

        create(projectJson("Portfolio", "DRAFT", ",\"technologyIds\":[999999]"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("technologyIds"));
        create(projectJson("Portfolio", "DRAFT", ",\"coverMediaId\":" + pdf))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("coverMediaId"));
        create(projectJson("Portfolio", "DRAFT", ",\"screenshots\":[{\"mediaId\":" + pdf + "}]"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("screenshots"));
        create(projectJson("Portfolio", "DRAFT",
            ",\"screenshots\":[{\"mediaId\":" + picture + "},{\"mediaId\":" + picture + "}]"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].message").value("Une capture figure deux fois."));
        assertThat(projectRepository.existsAny()).isFalse();
    }

    /**
     * Technologies et captures remplacées d'un bloc ; le slug est conservé sans saisie.
     */
    @Test
    void updates_a_project() throws Exception {
        Technology java = technologyRepository.create(technology("Java", "java", 0));
        Technology docker = technologyRepository.create(technology("Docker", "docker", 1));
        long first = image();
        long second = image();
        long id = projectRepository.create(project("portfolio", ProjectVisibility.DRAFT,
            DateRange.ongoingSince(START), 0, java)).id();

        update(id, projectJson("Portfolio relu", "DRAFT", """
            ,"technologyIds":[%d],"screenshots":[{"mediaId":%d},{"mediaId":%d}]
            """.formatted(docker.id(), first, second)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value("portfolio"))
            .andExpect(jsonPath("$.title").value("Portfolio relu"))
            .andExpect(jsonPath("$.technologyIds.length()").value(1))
            .andExpect(jsonPath("$.technologyIds[0]").value(docker.id()));
        update(id, projectJson("Portfolio relu", "DRAFT", """
            ,"slug":"mon-portfolio","screenshots":[{"mediaId":%d,"caption":"Nouvelle"}]
            """.formatted(second)))
            .andExpect(jsonPath("$.slug").value("mon-portfolio"))
            .andExpect(jsonPath("$.technologyIds.length()").value(0))
            .andExpect(jsonPath("$.screenshots.length()").value(1))
            .andExpect(jsonPath("$.screenshots[0].caption").value("Nouvelle"));
    }

    /**
     * D11 : une fois publié, un projet garde son slug, même archivé ; le reste de la saisie change.
     */
    @Test
    void locks_the_slug_once_published() throws Exception {
        long id = projectRepository.create(project("portfolio", ProjectVisibility.DRAFT,
            DateRange.ongoingSince(START), 0)).id();

        update(id, projectJson("Portfolio", "PUBLISHED", ",\"slug\":\"portfolio-public\""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value("portfolio-public"))
            .andExpect(jsonPath("$.slugLocked").value(true));
        update(id, projectJson("Portfolio", "ARCHIVED", ""))
            .andExpect(jsonPath("$.visibility").value("ARCHIVED"));
        update(id, projectJson("Portfolio", "DRAFT", ",\"slug\":\"autre\""))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SLUG_LOCKED"));
        mockMvc.perform(get("/api/public/projects/portfolio-public").contextPath("/api"))
            .andExpect(status().isNotFound());
    }

    @Test
    void lists_every_project_in_display_order() throws Exception {
        projectRepository.create(project("brouillon", ProjectVisibility.DRAFT, DateRange.ongoingSince(START), 2));
        projectRepository.create(published("public", START, 1));
        projectRepository.create(project("archive", ProjectVisibility.ARCHIVED, DateRange.ongoingSince(START), 3));

        mockMvc.perform(get("/api/admin/projects").contextPath("/api").with(user("admin")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content[0].slug").value("public"))
            .andExpect(jsonPath("$.content[1].slug").value("brouillon"))
            .andExpect(jsonPath("$.content[1].visibility").value("DRAFT"))
            .andExpect(jsonPath("$.content[2].slug").value("archive"));
        mockMvc.perform(get("/api/admin/projects/999999").contextPath("/api").with(user("admin")))
            .andExpect(status().isNotFound());
        update(999_999L, projectJson("Portfolio", "DRAFT", "")).andExpect(status().isNotFound());
    }

    /**
     * Invariant 28 : le document de recherche suit les technologies d'un projet publié.
     */
    @Test
    void the_search_follows_an_update() throws Exception {
        Technology kotlin = technologyRepository.create(technology("Kotlin", "kotlin", 0));
        long id = projectRepository.create(published("portfolio", START, 0)).id();

        update(id, projectJson("Portfolio", "PUBLISHED", ",\"technologyIds\":[" + kotlin.id() + "]"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/search?q=kotlin").contextPath("/api"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].slug").value("portfolio"));
    }

    @Test
    void translates_a_slug_taken_meanwhile() {
        projectRepository.create(published("portfolio", START, 0));

        assertThatThrownBy(() -> projectRepository.create(published("portfolio", START, 1)))
            .isInstanceOfSatisfying(BusinessRuleViolationException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.SLUG_ALREADY_USED));
    }

    private long image() {
        return mediaRepository.create(MediaFixtures.image(null)).id();
    }

    private ResultActions create(String json) throws Exception {
        return mockMvc.perform(post("/api/admin/projects").contextPath("/api").with(user("admin")).with(xsrf())
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions update(long id, String json) throws Exception {
        return mockMvc.perform(put("/api/admin/projects/" + id).contextPath("/api").with(user("admin")).with(xsrf())
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /**
     * Projet en cours depuis le 1er janvier 2026 ; {@code extra} ajoute des champs (commence par une virgule).
     */
    private static String projectJson(String title, String visibility, String extra) {
        return """
            {"title":"%s","shortDescription":"Résumé","descriptionMarkdown":"# Projet","stage":"IN_PROGRESS",
             "visibility":"%s","startDate":"2026-01-01"%s}
            """.formatted(title, visibility, extra);
    }

    private static String withPeriod(String stage, String startDate, String endDate) {
        return """
            {"title":"Portfolio","shortDescription":"Résumé","descriptionMarkdown":"","stage":"%s",
             "visibility":"DRAFT","startDate":"%s","endDate":%s}
            """.formatted(stage, startDate, endDate == null ? "null" : "\"" + endDate + "\"");
    }

    private long idOf(String slug) {
        return projectRepository.findPage(new com.scalke.portfolio.backend.shared.domain.model.PageQuery(0, 100))
            .content().stream()
            .filter(project -> project.slug().equals(Slug.of(slug)))
            .map(Project::id)
            .findFirst().orElseThrow();
    }
}
