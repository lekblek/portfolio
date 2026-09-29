package com.scalke.portfolio.backend.publication.web.controller;

import com.scalke.portfolio.backend.media.MediaFixtures;
import com.scalke.portfolio.backend.media.domain.port.MediaRepository;
import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.publication.domain.port.PublicationRepository;
import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;
import com.scalke.portfolio.backend.shared.error.InvalidInputException;
import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import com.scalke.portfolio.backend.taxonomy.domain.port.CategoryRepository;
import com.scalke.portfolio.backend.taxonomy.domain.port.TagRepository;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static com.scalke.portfolio.backend.publication.PublicationFixtures.article;
import static com.scalke.portfolio.backend.testsupport.CsrfTestSupport.xsrf;
import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
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
 * Administration des publications de bout en bout (D-CU) : session, CSRF, validation, références vérifiées, slug
 * généré, suffixé ou verrouillé, cycle éditorial par sa route, liste de tous les statuts, effet sur le site public.
 */
@Transactional
class AdminPublicationIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PublicationRepository publicationRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    MediaRepository mediaRepository;

    @Test
    void requires_the_administrator_session_and_the_csrf_token() throws Exception {
        mockMvc.perform(get("/api/admin/publications").contextPath("/api"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(post("/api/admin/publications").contextPath("/api").with(user("admin"))
                .contentType(MediaType.APPLICATION_JSON).content(articleJson("Titre")))
            .andExpect(status().isForbidden());
    }

    /**
     * Brouillon au slug généré depuis le titre, termes et couverture par identifiant, champ SEO vide retiré ; il
     * n'apparaît pas sur le site.
     */
    @Test
    void creates_a_draft() throws Exception {
        long category = categoryRepository.create(new Category(null, "Backend", Slug.of("backend"), null)).id();
        long java = tagRepository.create(new Tag(null, "Java", Slug.of("java"))).id();
        long spring = tagRepository.create(new Tag(null, "Spring", Slug.of("spring"))).id();
        long cover = mediaRepository.create(MediaFixtures.image("Schéma")).id();

        create("""
            {"type":"ARTICLE","title":" Construire une API REST ","summary":"Résumé","contentMarkdown":"# API",
             "featured":true,"categoryId":%d,"tagIds":[%d,%d],"coverMediaId":%d,"seoTitle":"  ",
             "seoDescription":"Description"}
            """.formatted(category, spring, java, cover))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", endsWith("/api/admin/publications/" + idOf("construire-une-api-rest"))))
            .andExpect(jsonPath("$.type").value("ARTICLE"))
            .andExpect(jsonPath("$.title").value("Construire une API REST"))
            .andExpect(jsonPath("$.slug").value("construire-une-api-rest"))
            .andExpect(jsonPath("$.slugLocked").value(false))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.publishedAt").value(nullValue()))
            .andExpect(jsonPath("$.featured").value(true))
            .andExpect(jsonPath("$.categoryId").value(category))
            .andExpect(jsonPath("$.tagIds[0]").value(Math.min(java, spring)))
            .andExpect(jsonPath("$.tagIds[1]").value(Math.max(java, spring)))
            .andExpect(jsonPath("$.coverMediaId").value(cover))
            .andExpect(jsonPath("$.seoTitle").value(nullValue()))
            .andExpect(jsonPath("$.seoDescription").value("Description"))
            .andExpect(jsonPath("$.createdAt").value(NOW.toString()));

        mockMvc.perform(get("/api/public/publications/construire-une-api-rest").contextPath("/api"))
            .andExpect(status().isNotFound());
    }

    /**
     * D-BD : un slug déjà pris, saisi ou généré, reçoit le premier suffixe libre.
     */
    @Test
    void suffixes_a_slug_already_taken() throws Exception {
        create(articleJson("Mon article")).andExpect(jsonPath("$.slug").value("mon-article"));
        create(articleJson("Mon article")).andExpect(jsonPath("$.slug").value("mon-article-2"));
        create("""
            {"type":"NEWS","title":"Autre","slug":"mon-article","summary":"Résumé","contentMarkdown":""}
            """).andExpect(jsonPath("$.slug").value("mon-article-3"));
    }

    @Test
    void validates_the_request() throws Exception {
        create("""
            {"type":"ARTICLE","title":"!!!","summary":"Résumé","contentMarkdown":""}
            """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("title"));
        create("""
            {"title":"Titre","summary":"Résumé","contentMarkdown":""}
            """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("type"));
        create("""
            {"type":"ARTICLE","title":"Titre","summary":"Résumé","contentMarkdown":"%s"}
            """.formatted("a".repeat(100_001)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("contentMarkdown"));
        create("""
            {"type":"ARTICLE","title":"Titre","slug":"Mauvais Slug","summary":"Résumé","contentMarkdown":""}
            """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("slug"));
        create("""
            {"type":"PODCAST","title":"Titre","summary":"Résumé","contentMarkdown":""}
            """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    /**
     * Une référence inconnue, ou un PDF en couverture, est une saisie invalide (400 sur le champ), pas une 500.
     */
    @Test
    void refuses_unknown_references_and_a_document_as_cover() throws Exception {
        long pdf = mediaRepository.create(MediaFixtures.pdf()).id();

        create(withReference("\"categoryId\":999999"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("categoryId"));
        create(withReference("\"tagIds\":[999999]"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("tagIds"));
        create(withReference("\"coverMediaId\":" + pdf))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("coverMediaId"))
            .andExpect(jsonPath("$.errors[0].message").value("Image de couverture inconnue."));
        assertThat(publicationRepository.existsAny()).isFalse();
    }

    /**
     * Sans slug saisi, le slug est conservé ; tant que la publication n'a jamais été publique, il peut changer.
     */
    @Test
    void updates_a_draft() throws Exception {
        create(articleJson("Brouillon"));
        long id = idOf("brouillon");

        update(id, """
            {"title":"Brouillon relu","summary":"Nouveau résumé","contentMarkdown":"# Relu","tagIds":[]}
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Brouillon relu"))
            .andExpect(jsonPath("$.slug").value("brouillon"))
            .andExpect(jsonPath("$.type").value("ARTICLE"))
            .andExpect(jsonPath("$.status").value("DRAFT"));
        update(id, """
            {"title":"Brouillon relu","slug":"brouillon-relu","summary":"Résumé","contentMarkdown":""}
            """)
            .andExpect(jsonPath("$.slug").value("brouillon-relu"));
        assertThat(publicationRepository.findById(id).orElseThrow().contentMarkdown()).isEmpty();
    }

    /**
     * D11, D-BC : après la première publication, le slug ne change plus ; le reste de la saisie, si.
     */
    @Test
    void locks_the_slug_once_published() throws Exception {
        create(articleJson("Publié bientôt"));
        long id = idOf("publie-bientot");
        changeStatus(id, "{\"status\":\"PUBLISHED\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.publishedAt").value(NOW.toString()))
            .andExpect(jsonPath("$.slugLocked").value(true));

        update(id, """
            {"title":"Publié","slug":"autre-adresse","summary":"Résumé","contentMarkdown":""}
            """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SLUG_LOCKED"));
        update(id, """
            {"title":"Publié et corrigé","slug":"publie-bientot","summary":"Résumé","contentMarkdown":""}
            """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Publié et corrigé"));
        mockMvc.perform(get("/api/public/publications/publie-bientot").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Publié et corrigé"));
    }

    /**
     * D-AU, D-AV : les transitions passent par la table du domaine ; une planification exige une date future.
     */
    @Test
    void changes_the_status_through_the_editorial_cycle() throws Exception {
        create(articleJson("Planifié"));
        long id = idOf("planifie");
        Instant tomorrow = NOW.plus(Duration.ofDays(1));

        changeStatus(id, "{\"status\":\"SCHEDULED\",\"publishedAt\":\"" + tomorrow + "\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SCHEDULED"))
            .andExpect(jsonPath("$.publishedAt").value(tomorrow.toString()))
            .andExpect(jsonPath("$.slugLocked").value(false));
        changeStatus(id, "{\"status\":\"ARCHIVED\"}")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVALID_PUBLICATION_TRANSITION"));
        changeStatus(id, "{\"status\":\"SCHEDULED\",\"publishedAt\":\"" + NOW.minusSeconds(1) + "\"}")
            .andExpect(status().isConflict());
        changeStatus(id, "{}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("status"));
    }

    /**
     * Tous les statuts, les dernières modifiées d'abord, l'identifiant départageant les égalités ; une planification
     * échue apparaît publiée (D03).
     */
    @Test
    void lists_every_publication_most_recently_updated_first() throws Exception {
        Publication due = publicationRepository.create(article("echue", PublicationStatus.SCHEDULED,
            NOW.minus(Duration.ofHours(1))));
        Publication draft = publicationRepository.create(article("brouillon", PublicationStatus.DRAFT, null));
        Publication old = publicationRepository.create(updatedAt(article("ancienne", PublicationStatus.DRAFT, null),
            NOW.minus(Duration.ofDays(2))));

        mockMvc.perform(get("/api/admin/publications").contextPath("/api").with(user("admin")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content[0].id").value(draft.id()))
            .andExpect(jsonPath("$.content[0].status").value("DRAFT"))
            .andExpect(jsonPath("$.content[1].id").value(due.id()))
            .andExpect(jsonPath("$.content[1].status").value("PUBLISHED"))
            .andExpect(jsonPath("$.content[2].id").value(old.id()))
            .andExpect(jsonPath("$.content[2].contentMarkdown").doesNotExist());
    }

    @Test
    void reads_one_publication_whatever_its_status() throws Exception {
        Publication draft = publicationRepository.create(article("brouillon", PublicationStatus.DRAFT, null));

        mockMvc.perform(get("/api/admin/publications/" + draft.id()).contextPath("/api").with(user("admin")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contentMarkdown").value(draft.contentMarkdown()));
        mockMvc.perform(get("/api/admin/publications/999999").contextPath("/api").with(user("admin")))
            .andExpect(status().isNotFound());
        update(999_999L, articleJson("Titre")).andExpect(status().isNotFound());
        changeStatus(999_999L, "{\"status\":\"PUBLISHED\"}").andExpect(status().isNotFound());
    }

    /**
     * Invariant 28 : le document de recherche suit la modification des tags (déclencheur de {@code V018}).
     */
    @Test
    void the_search_follows_an_update() throws Exception {
        create(articleJson("Article"));
        long id = idOf("article");
        changeStatus(id, "{\"status\":\"PUBLISHED\"}");
        long kotlin = tagRepository.create(new Tag(null, "Kotlin", Slug.of("kotlin"))).id();

        update(id, """
            {"title":"Article","summary":"Résumé","contentMarkdown":"","tagIds":[%d]}
            """.formatted(kotlin)).andExpect(status().isOk());

        mockMvc.perform(get("/api/public/search?q=kotlin").contextPath("/api"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].slug").value("article"));
    }

    /**
     * Écriture concurrente : un refus de PostgreSQL survenu après les vérifications est traduit, pas une 500.
     */
    @Test
    void translates_a_constraint_violation_that_slipped_past_the_checks() {
        Publication unknownCategory = new Publication(null, PublicationType.ARTICLE, "Titre", Slug.of("article"),
            "Résumé", "", PublicationStatus.DRAFT, null, null, false, 999_999L, Set.of(), null, null, NOW, NOW, null);

        assertThatThrownBy(() -> publicationRepository.create(unknownCategory))
            .isInstanceOfSatisfying(InvalidInputException.class, exception ->
                assertThat(exception.field()).isEqualTo("categoryId"));
    }

    @Test
    void translates_a_slug_taken_meanwhile() {
        publicationRepository.create(article("article", PublicationStatus.DRAFT, null));

        assertThatThrownBy(() -> publicationRepository.create(article("article", PublicationStatus.DRAFT, null)))
            .isInstanceOfSatisfying(BusinessRuleViolationException.class, exception ->
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.SLUG_ALREADY_USED));
    }

    @Test
    void translates_a_tag_deleted_meanwhile() {
        assertThatThrownBy(() -> publicationRepository.create(draft(Set.of(999_999L), null)))
            .isInstanceOfSatisfying(InvalidInputException.class, exception ->
                assertThat(exception.field()).isEqualTo("tagIds"));
    }

    @Test
    void translates_a_cover_deleted_meanwhile() {
        assertThatThrownBy(() -> publicationRepository.create(draft(Set.of(), 999_999L)))
            .isInstanceOfSatisfying(InvalidInputException.class, exception ->
                assertThat(exception.field()).isEqualTo("coverMediaId"));
    }

    private static Publication draft(Set<Long> tagIds, Long coverMediaId) {
        return new Publication(null, PublicationType.ARTICLE, "Titre", Slug.of("article"), "Résumé", "",
            PublicationStatus.DRAFT, null, null, false, null, tagIds, null, null, NOW, NOW, coverMediaId);
    }

    private ResultActions create(String json) throws Exception {
        return mockMvc.perform(post("/api/admin/publications").contextPath("/api").with(user("admin")).with(xsrf())
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions update(long id, String json) throws Exception {
        return mockMvc.perform(put("/api/admin/publications/" + id).contextPath("/api").with(user("admin"))
            .with(xsrf()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions changeStatus(long id, String json) throws Exception {
        return mockMvc.perform(post("/api/admin/publications/" + id + "/status").contextPath("/api")
            .with(user("admin")).with(xsrf()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String articleJson(String title) {
        return """
            {"type":"ARTICLE","title":"%s","summary":"Résumé","contentMarkdown":"# Contenu"}
            """.formatted(title);
    }

    private static String withReference(String reference) {
        return """
            {"type":"ARTICLE","title":"Titre","summary":"Résumé","contentMarkdown":"",%s}
            """.formatted(reference);
    }

    private static Publication updatedAt(Publication publication, Instant updatedAt) {
        return new Publication(null, publication.type(), publication.title(), publication.slug(),
            publication.summary(), publication.contentMarkdown(), publication.status(), publication.publishedAt(),
            publication.firstPublishedAt(), publication.featured(), publication.categoryId(), publication.tagIds(),
            publication.seoTitle(), publication.seoDescription(), updatedAt, updatedAt, publication.coverMediaId());
    }

    private long idOf(String slug) {
        return publicationRepository.findBySlug(Slug.of(slug)).orElseThrow().id();
    }
}
