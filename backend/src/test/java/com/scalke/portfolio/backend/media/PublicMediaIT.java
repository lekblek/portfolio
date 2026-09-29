package com.scalke.portfolio.backend.media;

import com.scalke.portfolio.backend.media.application.usecase.MediaUpload;
import com.scalke.portfolio.backend.media.application.usecase.UploadMediaUseCase;
import com.scalke.portfolio.backend.media.domain.model.StorageKey;
import com.scalke.portfolio.backend.media.infrastructure.storage.MediaStorageProperties;
import com.scalke.portfolio.backend.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parcours complet : envoi (cas d'usage ; la route est testée par {@code AdminMediaIT}) puis lecture HTTP, sur le stockage
 * local réel, dans la racine du profil {@code test} ({@code target/test-media}).
 */
class PublicMediaIT extends AbstractIntegrationTest {

    private static final String KEY = "0123456789abcdef0123456789abcdef.png";
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MediaStorageProperties properties;

    @Autowired
    UploadMediaUseCase uploadMediaUseCase;

    @Autowired
    JdbcClient jdbcClient;

    private Path file;

    @BeforeEach
    void givenAStoredFile() throws IOException {
        Files.createDirectories(properties.storageRoot());
        file = Files.write(properties.storageRoot().resolve(KEY), PNG_SIGNATURE);
    }

    @AfterEach
    void deleteTheFile() throws IOException {
        Files.deleteIfExists(file);
    }

    @Test
    void serves_a_stored_file() throws Exception {
        mockMvc.perform(get("/api/public/media/" + KEY).contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(content().bytes(PNG_SIGNATURE));
    }

    @Test
    void serves_an_uploaded_file_under_its_new_key() throws Exception {
        byte[] pdf = "%PDF-1.7 curriculum vitae".getBytes();
        StorageKey key = uploadMediaUseCase.execute(new MediaUpload("cv.pdf", null, new ByteArrayInputStream(pdf)))
            .storageKey();
        try {
            mockMvc.perform(get("/api/public/media/" + key.value()).contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdf));
        } finally {
            Files.deleteIfExists(properties.storageRoot().resolve(key.value()));
            jdbcClient.sql("DELETE FROM media WHERE storage_key = :key").param("key", key.value()).update();
        }
    }

    @Test
    void hides_unknown_and_malformed_keys_behind_the_same_404() throws Exception {
        for (String key : new String[]{"fedcba9876543210fedcba9876543210.png", "application.yaml"}) {
            mockMvc.perform(get("/api/public/media/" + key).contextPath("/api"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Média introuvable."));
        }
    }
}
