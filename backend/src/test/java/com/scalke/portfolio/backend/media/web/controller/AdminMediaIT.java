package com.scalke.portfolio.backend.media.web.controller;

import com.scalke.portfolio.backend.media.MediaFixtures;
import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.port.MediaRepository;
import com.scalke.portfolio.backend.media.infrastructure.storage.MediaStorageProperties;
import com.scalke.portfolio.backend.project.domain.port.ProjectRepository;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static com.scalke.portfolio.backend.media.MediaSamples.PDF;
import static com.scalke.portfolio.backend.media.MediaSamples.PNG;
import static com.scalke.portfolio.backend.media.MediaSamples.ofSize;
import static com.scalke.portfolio.backend.project.ProjectFixtures.published;
import static com.scalke.portfolio.backend.project.ProjectFixtures.withImages;
import static com.scalke.portfolio.backend.testsupport.CsrfTestSupport.xsrf;
import static com.scalke.portfolio.backend.testsupport.FixedClockConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Administration du catalogue de bout en bout (D-CT), sur PostgreSQL et le stockage local du profil {@code test}.
 * Pas de transaction de test : une suppression refusée par une clé étrangère interrompt la transaction PostgreSQL
 * (comme {@code DeleteMediaUseCaseIT}) ; les entrées et les fichiers créés sont supprimés après chaque test. La limite
 * de la requête multipart, appliquée par Tomcat, se vérifie sur un vrai serveur ({@code HttpServerSecurityIT}).
 */
class AdminMediaIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MediaRepository mediaRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    MediaStorageProperties properties;

    @Autowired
    JdbcClient jdbcClient;

    @AfterEach
    void deleteWhatWasCreated() throws IOException {
        jdbcClient.sql("DELETE FROM project").update();
        for (String key : jdbcClient.sql("SELECT storage_key FROM media").query(String.class).list()) {
            Files.deleteIfExists(properties.storageRoot().resolve(key));
        }
        jdbcClient.sql("DELETE FROM media").update();
    }

    @Test
    void requires_the_administrator_session_and_the_csrf_token() throws Exception {
        mockMvc.perform(get("/api/admin/media").contextPath("/api"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(multipart("/api/admin/media").file(file("logo.png", PNG)).contextPath("/api").with(user("admin")))
            .andExpect(status().isForbidden());
    }

    /**
     * Format reconnu par le contenu, dimensions lues, texte alternatif nettoyé, fichier servi à l'adresse publique.
     */
    @Test
    void uploads_an_image_and_serves_it() throws Exception {
        upload(file("C:\\images\\logo.png", PNG), " Logo du site ")
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", endsWith("/api/admin/media/" + onlyMedia().id())))
            .andExpect(jsonPath("$.id").value(onlyMedia().id()))
            .andExpect(jsonPath("$.originalName").value("logo.png"))
            .andExpect(jsonPath("$.format").value("PNG"))
            .andExpect(jsonPath("$.mimeType").value("image/png"))
            .andExpect(jsonPath("$.sizeBytes").value(PNG.length))
            .andExpect(jsonPath("$.width").value(3))
            .andExpect(jsonPath("$.height").value(2))
            .andExpect(jsonPath("$.altText").value("Logo du site"))
            .andExpect(jsonPath("$.createdAt").value(NOW.toString()));

        mockMvc.perform(get("/api/public/media/" + onlyMedia().storageKey().value()).contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(content().bytes(PNG));
    }

    @Test
    void uploads_a_pdf_without_dimensions() throws Exception {
        upload(file("cv.pdf", PDF), "  ")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.format").value("PDF"))
            .andExpect(jsonPath("$.width").value(nullValue()))
            .andExpect(jsonPath("$.height").value(nullValue()))
            .andExpect(jsonPath("$.altText").value(nullValue()));
    }

    @Test
    void refuses_an_unsupported_or_oversized_file() throws Exception {
        upload(file("notes.txt", "pas une image".getBytes()), null)
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_FORMAT"));
        upload(file("grande.png", ofSize(PNG, 5 * 1024 * 1024 + 1)), null)
            .andExpect(status().isContentTooLarge())
            .andExpect(jsonPath("$.code").value("MEDIA_TOO_LARGE"));
        assertThat(mediaRepository.findAllById(allIds())).isEmpty();
    }

    @Test
    void validates_the_upload() throws Exception {
        upload(file("logo.png", PNG), "a".repeat(301))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("altText"));
        mockMvc.perform(multipart("/api/admin/media").contextPath("/api").with(user("admin")).with(xsrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    /**
     * Les plus récents d'abord : la date de création prime sur l'identifiant, qui départage les égalités.
     */
    @Test
    void lists_the_newest_media_first() throws Exception {
        Media recent = mediaRepository.create(MediaFixtures.image("récent"));
        Media sameTime = mediaRepository.create(MediaFixtures.pdf());
        Media older = mediaRepository.create(createdAt(MediaFixtures.image("ancien"), NOW.minus(Duration.ofDays(1))));

        mockMvc.perform(get("/api/admin/media").contextPath("/api").with(user("admin")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content[0].id").value(sameTime.id()))
            .andExpect(jsonPath("$.content[1].id").value(recent.id()))
            .andExpect(jsonPath("$.content[2].id").value(older.id()))
            .andExpect(jsonPath("$.content[2].url").value("/api/public/media/" + older.storageKey().value()));
        mockMvc.perform(get("/api/admin/media?page=1&size=2").contextPath("/api").with(user("admin")))
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].id").value(older.id()));
    }

    @Test
    void reads_one_media() throws Exception {
        Media media = mediaRepository.create(MediaFixtures.image("Schéma"));

        mockMvc.perform(get("/api/admin/media/" + media.id()).contextPath("/api").with(user("admin")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.altText").value("Schéma"))
            .andExpect(jsonPath("$.width").value(1200));
        mockMvc.perform(get("/api/admin/media/999999").contextPath("/api").with(user("admin")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    /**
     * Seul le texte alternatif change ; vide, il est retiré.
     */
    @Test
    void updates_the_alt_text() throws Exception {
        Media media = mediaRepository.create(MediaFixtures.image("Ancien"));

        updateAltText(media.id(), "{\"altText\":\" Nouveau \"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.altText").value("Nouveau"))
            .andExpect(jsonPath("$.originalName").value("image.webp"));
        assertThat(mediaRepository.findById(media.id()).orElseThrow().altText()).isEqualTo("Nouveau");

        updateAltText(media.id(), "{\"altText\":\"\"}")
            .andExpect(jsonPath("$.altText").value(nullValue()));
        assertThat(mediaRepository.findById(media.id()).orElseThrow().altText()).isNull();
    }

    @Test
    void validates_the_alt_text_update() throws Exception {
        Media media = mediaRepository.create(MediaFixtures.image(null));

        updateAltText(media.id(), "{\"altText\":\"" + "a".repeat(301) + "\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("altText"));
        updateAltText(999_999L, "{\"altText\":\"Texte\"}")
            .andExpect(status().isNotFound());
    }

    @Test
    void deletes_an_unused_media_and_its_file() throws Exception {
        upload(file("logo.png", PNG), null).andExpect(status().isCreated());
        Media media = onlyMedia();

        mockMvc.perform(delete("/api/admin/media/" + media.id()).contextPath("/api").with(user("admin")).with(xsrf()))
            .andExpect(status().isNoContent());
        assertThat(mediaRepository.findById(media.id())).isEmpty();
        assertThat(properties.storageRoot().resolve(media.storageKey().value())).doesNotExist();
    }

    /**
     * Invariant 13 : un média utilisé par un contenu n'est pas supprimé.
     */
    @Test
    void refuses_to_delete_a_media_in_use() throws Exception {
        Media cover = mediaRepository.create(MediaFixtures.image("Couverture"));
        projectRepository.create(withImages(published("avec-couverture", LocalDate.of(2026, 1, 1), 0), cover.id()));

        mockMvc.perform(delete("/api/admin/media/" + cover.id()).contextPath("/api").with(user("admin")).with(xsrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("MEDIA_STILL_REFERENCED"));
        assertThat(mediaRepository.findById(cover.id())).isPresent();
    }

    private ResultActions upload(MockMultipartFile file, String altText) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/admin/media").file(file);
        if (altText != null) {
            request.param("altText", altText);
        }
        return mockMvc.perform(request.contextPath("/api").with(user("admin")).with(xsrf()));
    }

    private ResultActions updateAltText(long id, String json) throws Exception {
        return mockMvc.perform(patch("/api/admin/media/" + id).contextPath("/api").with(user("admin")).with(xsrf())
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, MediaType.APPLICATION_OCTET_STREAM_VALUE, content);
    }

    private static Media createdAt(Media media, Instant createdAt) {
        return new Media(null, media.storageKey(), media.originalName(), media.size(), media.dimensions(),
            media.altText(), createdAt);
    }

    private Media onlyMedia() {
        return mediaRepository.findAllById(allIds()).getFirst();
    }

    private List<Long> allIds() {
        return jdbcClient.sql("SELECT id FROM media").query(Long.class).list();
    }
}
